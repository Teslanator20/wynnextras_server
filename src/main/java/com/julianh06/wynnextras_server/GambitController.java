package com.julianh06.wynnextras_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/gambit")
public class GambitController {
    private static final Logger logger = LoggerFactory.getLogger(GambitController.class);

    @Autowired
    private GambitService gambitService;

    @Autowired
    private WynncraftService wynncraftService;

    /**
     * Submit today's gambits
     * POST /gambit
     * Header: Wynncraft-Api-Key (required)
     * Body: { "gambits": [{"name": "...", "description": "..."}] }
     */
    @PostMapping
    public ResponseEntity<?> submitGambits(
            @RequestBody GambitSubmissionDto submission,
            @RequestHeader("Wynncraft-Api-Key") String apiKey) {

        // Validate API key and get username
        List<String> uuids;
        try {
            uuids = wynncraftService.fetchUuid(apiKey);
        } catch (Exception e) {
            logger.error("Failed to validate API key", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Wynncraft API key");
        }

        if (uuids == null || uuids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Wynncraft API key");
        }

        // Get username from first UUID
        String username = uuids.get(0);

        // Submit gambits
        try {
            GambitSubmissionDto approved = gambitService.submitGambits(
                submission.getGambits(),
                username
            );

            if (approved != null) {
                logger.info("Gambits were approved");
                return ResponseEntity.ok().body(createResponse(
                    "approved",
                    "Gambits approved for today",
                    approved
                ));
            } else {
                logger.info("Gambits submitted but not yet approved");
                return ResponseEntity.ok().body(createResponse(
                    "submitted",
                    "Gambits submitted. Waiting for more confirmations.",
                    null
                ));
            }
        } catch (Exception e) {
            logger.error("Error submitting gambits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing submission");
        }
    }

    /**
     * Get today's approved gambits
     * GET /gambit
     */
    @GetMapping
    public ResponseEntity<?> getGambits() {
        GambitSubmissionDto gambits = gambitService.getApprovedGambits();

        if (gambits != null) {
            return ResponseEntity.ok(gambits);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved gambits for today");
        }
    }

    private java.util.Map<String, Object> createResponse(String status, String message, Object data) {
        java.util.Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        if (data != null) {
            response.put("gambits", data);
        }
        return response;
    }
}
