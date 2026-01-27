package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.service.VerifiedUserLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints for server management
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private VerifiedUserLoader verifiedUserLoader;

    /**
     * Reload verified users from file
     * POST /admin/reload-verified-users
     *
     * No authentication required for now - add if needed
     */
    @PostMapping("/reload-verified-users")
    public ResponseEntity<?> reloadVerifiedUsers() {
        try {
            logger.info("Admin triggered verified users reload");
            verifiedUserLoader.loadVerifiedUsers();

            long count = verifiedUserLoader.getVerifiedUserCount();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Verified users reloaded successfully");
            response.put("verifiedUserCount", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reloading verified users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to reload verified users: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get verified users count
     * GET /admin/verified-users/count
     */
    @GetMapping("/verified-users/count")
    public ResponseEntity<?> getVerifiedUserCount() {
        long count = verifiedUserLoader.getVerifiedUserCount();

        Map<String, Object> response = new HashMap<>();
        response.put("verifiedUserCount", count);

        return ResponseEntity.ok(response);
    }
}
