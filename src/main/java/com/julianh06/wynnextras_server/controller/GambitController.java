package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.GambitSubmissionDto;
import com.julianh06.wynnextras_server.service.GambitService;
import com.julianh06.wynnextras_server.service.MojangAuthService;
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
    private MojangAuthService mojangAuth;

    /**
     * Submit today's gambits
     * POST /gambit
     * Headers:
     *   - Username (required) - Minecraft username
     *   - Server-ID (required) - Shared secret for Mojang verification
     * Body: { "gambits": [{"name": "...", "description": "..."}] }
     */
    @PostMapping
    public ResponseEntity<?> submitGambits(
            @RequestBody GambitSubmissionDto submission,
            @RequestHeader("Username") String username,
            @RequestHeader("Server-ID") String serverId) {

        // Validate headers
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing Username header");
        }
        if (serverId == null || serverId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing Server-ID header");
        }

        // Authenticate with Mojang
        MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
        if (!authResult.isSuccess()) {
            logger.warn("Authentication failed for user {}: {}", username, authResult.getError());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createResponse("error", authResult.getError(), null));
        }

        String verifiedUsername = authResult.getUsername();

        // Submit gambits
        try {
            GambitSubmissionDto approved = gambitService.submitGambits(
                submission.getGambits(),
                verifiedUsername
            );

            if (approved != null) {
                logger.info("Gambits were approved (submitted by {})", verifiedUsername);
                return ResponseEntity.ok().body(createResponse(
                    "approved",
                    "Gambits approved for today",
                    approved
                ));
            } else {
                logger.info("Gambits submitted by {} but not yet approved", verifiedUsername);
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
