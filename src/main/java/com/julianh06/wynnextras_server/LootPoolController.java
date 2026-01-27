package com.julianh06.wynnextras_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lootpool")
public class LootPoolController {
    private static final Logger logger = LoggerFactory.getLogger(LootPoolController.class);

    @Autowired
    private LootPoolService lootPoolService;

    @Autowired
    private WynncraftService wynncraftService;

    /**
     * Submit a loot pool for a raid
     * POST /lootpool/{raidType}
     * Header: Player-UUID (required) - The Minecraft player's UUID
     * Body: { "aspects": [{"name": "...", "rarity": "...", "requiredClass": "..."}] }
     */
    @PostMapping("/{raidType}")
    public ResponseEntity<?> submitLootPool(
            @PathVariable String raidType,
            @RequestBody LootPoolSubmissionDto submission,
            @RequestHeader("Player-UUID") String playerUuid) {

        // Validate raid type
        if (!isValidRaidType(raidType)) {
            logger.warn("Invalid raid type: {}", raidType);
            return ResponseEntity.badRequest().body("Invalid raid type. Must be NOTG, NOL, TCC, or TNA");
        }

        // Validate UUID format
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            logger.warn("Missing Player-UUID header");
            return ResponseEntity.badRequest().body("Missing Player-UUID header");
        }

        // Normalize UUID (remove dashes if present)
        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();

        // Validate UUID format (32 hex characters without dashes, or 36 with dashes)
        if (!normalizedUuid.matches("[0-9a-f]{32}")) {
            logger.warn("Invalid UUID format: {}", playerUuid);
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }

        // Use normalized UUID as username
        String username = normalizedUuid;

        // Submit loot pool
        try {
            LootPoolSubmissionDto approved = lootPoolService.submitLootPool(
                raidType,
                submission.getAspects(),
                username
            );

            if (approved != null) {
                logger.info("Loot pool for {} was approved", raidType);
                return ResponseEntity.ok().body(Map.of(
                    "status", "approved",
                    "message", "Loot pool approved for " + raidType,
                    "lootPool", approved
                ));
            } else {
                logger.info("Loot pool for {} submitted but not yet approved", raidType);
                return ResponseEntity.ok().body(Map.of(
                    "status", "submitted",
                    "message", "Loot pool submitted. Waiting for more confirmations."
                ));
            }
        } catch (Exception e) {
            logger.error("Error submitting loot pool", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing submission");
        }
    }

    /**
     * Get the approved loot pool for a raid
     * GET /lootpool/{raidType}
     */
    @GetMapping("/{raidType}")
    public ResponseEntity<?> getLootPool(@PathVariable String raidType) {
        if (!isValidRaidType(raidType)) {
            return ResponseEntity.badRequest().body("Invalid raid type. Must be NOTG, NOL, TCC, or TNA");
        }

        LootPoolSubmissionDto lootPool = lootPoolService.getApprovedLootPool(raidType);

        if (lootPool != null) {
            return ResponseEntity.ok(lootPool);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved loot pool for " + raidType);
        }
    }

    private boolean isValidRaidType(String raidType) {
        return raidType.equals("NOTG") || raidType.equals("NOL") ||
               raidType.equals("TCC") || raidType.equals("TNA");
    }

    private static class Map<K, V> extends java.util.HashMap<K, V> {
        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            return map;
        }

        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            return map;
        }
    }
}
