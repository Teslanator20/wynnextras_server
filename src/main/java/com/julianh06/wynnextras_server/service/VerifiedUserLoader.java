package com.julianh06.wynnextras_server.service;

import com.julianh06.wynnextras_server.entity.VerifiedUser;
import com.julianh06.wynnextras_server.repository.VerifiedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads verified users from verified_users.txt on startup
 * Syncs with database to keep verified user list up to date
 */
@Service
public class VerifiedUserLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(VerifiedUserLoader.class);
    private static final String VERIFIED_USERS_FILE = "verified_users.txt";

    @Autowired
    private VerifiedUserRepository verifiedUserRepo;

    @Override
    public void run(String... args) {
        logger.info("Loading verified users from {}", VERIFIED_USERS_FILE);
        loadVerifiedUsers();
    }

    /**
     * Load verified users from file and sync to database
     */
    public void loadVerifiedUsers() {
        try {
            ClassPathResource resource = new ClassPathResource(VERIFIED_USERS_FILE);
            if (!resource.exists()) {
                logger.warn("Verified users file not found: {}", VERIFIED_USERS_FILE);
                return;
            }

            Set<String> fileUsernames = new HashSet<>();

            // Read file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Normalize username (case-insensitive)
                    String username = line.toLowerCase();
                    fileUsernames.add(username);
                    logger.debug("Line {}: Added verified user '{}'", lineNumber, username);
                }
            }

            // Sync to database
            int added = 0;
            int existing = 0;

            for (String username : fileUsernames) {
                if (!verifiedUserRepo.existsByUsername(username)) {
                    VerifiedUser user = new VerifiedUser(username);
                    verifiedUserRepo.save(user);
                    added++;
                    logger.info("Added new verified user: {}", username);
                } else {
                    existing++;
                }
            }

            // Remove users from database that are no longer in file
            int removed = 0;
            for (VerifiedUser dbUser : verifiedUserRepo.findAll()) {
                if (!fileUsernames.contains(dbUser.getUsername().toLowerCase())) {
                    verifiedUserRepo.delete(dbUser);
                    removed++;
                    logger.info("Removed verified user (no longer in file): {}", dbUser.getUsername());
                }
            }

            logger.info("Verified users loaded: {} total ({} added, {} existing, {} removed)",
                fileUsernames.size(), added, existing, removed);

        } catch (Exception e) {
            logger.error("Error loading verified users from file", e);
        }
    }

    /**
     * Get count of verified users
     */
    public long getVerifiedUserCount() {
        return verifiedUserRepo.count();
    }
}
