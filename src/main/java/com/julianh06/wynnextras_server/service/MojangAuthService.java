package com.julianh06.wynnextras_server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for authenticating players via Mojang's sessionserver
 * Uses the standard Minecraft authentication flow without exposing session IDs
 */
@Service
public class MojangAuthService {
    private static final Logger logger = LoggerFactory.getLogger(MojangAuthService.class);
    private static final String MOJANG_SESSION_SERVER = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cache to prevent replay attacks - serverId can only be used once within 30 seconds
    private final Map<String, Long> usedServerIds = new ConcurrentHashMap<>();
    private static final long SERVER_ID_EXPIRY_MS = 30000; // 30 seconds

    /**
     * Verify a player's authentication with Mojang
     *
     * @param username The player's username
     * @param serverId The shared secret used in the authentication handshake
     * @return AuthResult with verified UUID and username, or error
     */
    public AuthResult verifyPlayer(String username, String serverId) {
        // Check if serverId was already used (prevent replay attacks)
        Long lastUsed = usedServerIds.get(serverId);
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < SERVER_ID_EXPIRY_MS) {
            logger.warn("Replay attack detected: serverId {} already used", serverId);
            return AuthResult.error("Authentication token already used");
        }

        try {
            // Call Mojang's sessionserver to verify the player
            String url = MOJANG_SESSION_SERVER +
                "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                "&serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse response
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String verifiedUuid = json.get("id").getAsString();
                String verifiedUsername = json.get("name").getAsString();

                // Normalize UUID (remove dashes if present, lowercase)
                verifiedUuid = verifiedUuid.replace("-", "").toLowerCase();

                // Mark serverId as used
                usedServerIds.put(serverId, System.currentTimeMillis());
                cleanupExpiredServerIds();

                logger.info("Successfully authenticated player {} (UUID: {})", verifiedUsername, verifiedUuid);
                return AuthResult.success(verifiedUuid, verifiedUsername);

            } else if (response.statusCode() == 204) {
                // No Content - authentication failed
                logger.warn("Authentication failed for username {}: Invalid session", username);
                return AuthResult.error("Authentication failed - invalid session");

            } else {
                logger.error("Mojang sessionserver returned unexpected status: {}", response.statusCode());
                return AuthResult.error("Authentication service error");
            }

        } catch (Exception e) {
            logger.error("Error verifying player with Mojang", e);
            return AuthResult.error("Authentication failed - " + e.getMessage());
        }
    }

    /**
     * Clean up expired serverIds to prevent memory leak
     */
    private void cleanupExpiredServerIds() {
        if (usedServerIds.size() > 1000) { // Only cleanup if map gets large
            long now = System.currentTimeMillis();
            usedServerIds.entrySet().removeIf(entry ->
                now - entry.getValue() > SERVER_ID_EXPIRY_MS);
        }
    }

    /**
     * Result of authentication attempt
     */
    public static class AuthResult {
        private final boolean success;
        private final String uuid;
        private final String username;
        private final String error;

        private AuthResult(boolean success, String uuid, String username, String error) {
            this.success = success;
            this.uuid = uuid;
            this.username = username;
            this.error = error;
        }

        public static AuthResult success(String uuid, String username) {
            return new AuthResult(true, uuid, username, null);
        }

        public static AuthResult error(String error) {
            return new AuthResult(false, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getError() { return error; }
    }
}
