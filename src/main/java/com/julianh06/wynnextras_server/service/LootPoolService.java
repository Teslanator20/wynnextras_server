package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianh06.wynnextras_server.dto.LootPoolSubmissionDto;
import com.julianh06.wynnextras_server.entity.RaidLootPoolApproved;
import com.julianh06.wynnextras_server.entity.RaidLootPoolSubmission;
import com.julianh06.wynnextras_server.repository.RaidLootPoolApprovedRepository;
import com.julianh06.wynnextras_server.repository.RaidLootPoolSubmissionRepository;
import com.julianh06.wynnextras_server.repository.VerifiedUserRepository;
import com.julianh06.wynnextras_server.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LootPoolService {
    private static final Logger logger = LoggerFactory.getLogger(LootPoolService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RaidLootPoolSubmissionRepository submissionRepo;

    @Autowired
    private RaidLootPoolApprovedRepository approvedRepo;

    @Autowired
    private VerifiedUserRepository verifiedUserRepo;

    /**
     * Submit a loot pool for a raid
     * Returns the approved pool if submission triggers approval, null otherwise
     */
    @Transactional
    public LootPoolSubmissionDto submitLootPool(String raidType, List<LootPoolSubmissionDto.AspectDto> aspects, String username) {
        String weekId = TimeUtils.getWeekIdentifier();

        // Check if already approved and locked for this week
        Optional<RaidLootPoolApproved> existing = approvedRepo.findByRaidTypeAndWeekIdentifier(raidType, weekId);
        if (existing.isPresent() && existing.get().isLocked()) {
            logger.info("Loot pool for {} week {} is locked, ignoring submission from {}", raidType, weekId, username);
            return deserializeAspects(existing.get().getAspectsJson());
        }

        // Sort aspects by name for comparison
        List<LootPoolSubmissionDto.AspectDto> sortedAspects = aspects.stream()
            .sorted(Comparator.comparing(LootPoolSubmissionDto.AspectDto::getName))
            .collect(Collectors.toList());

        String aspectsJson = serializeAspects(sortedAspects);

        // Save submission
        RaidLootPoolSubmission submission = new RaidLootPoolSubmission(raidType, username, aspectsJson, weekId);
        submissionRepo.save(submission);
        logger.info("Saved loot pool submission for {} week {} from {}", raidType, weekId, username);

        // Check if should approve
        return checkAndApprove(raidType, weekId, aspectsJson, username);
    }

    /**
     * Get the approved loot pool for a raid in the current week
     */
    public LootPoolSubmissionDto getApprovedLootPool(String raidType) {
        String weekId = TimeUtils.getWeekIdentifier();
        Optional<RaidLootPoolApproved> approved = approvedRepo.findByRaidTypeAndWeekIdentifier(raidType, weekId);

        if (approved.isPresent()) {
            return deserializeAspects(approved.get().getAspectsJson());
        }

        return null;
    }

    /**
     * Check if username is in verified user list
     */
    public boolean isVerifiedUser(String username) {
        return verifiedUserRepo.existsByUsername(username);
    }

    /**
     * Check if this submission should trigger approval
     * Returns the approved pool if approved, null otherwise
     */
    @Transactional
    private LootPoolSubmissionDto checkAndApprove(String raidType, String weekId, String aspectsJson, String submittingUsername) {
        // Check if verified user - instant approval
        if (isVerifiedUser(submittingUsername)) {
            logger.info("Verified user {} submitted loot pool for {} week {}, auto-approving", submittingUsername, raidType, weekId);
            RaidLootPoolApproved approved = new RaidLootPoolApproved(raidType, aspectsJson, weekId, 1, false);
            approvedRepo.save(approved);
            return deserializeAspects(aspectsJson);
        }

        // Get all submissions for this raid/week with matching JSON
        List<RaidLootPoolSubmission> allSubmissions = submissionRepo.findByRaidTypeAndWeekIdentifier(raidType, weekId);
        List<RaidLootPoolSubmission> matchingSubmissions = allSubmissions.stream()
            .filter(s -> s.getAspectsJson().equals(aspectsJson))
            .collect(Collectors.toList());

        int matchCount = matchingSubmissions.size();
        logger.info("Found {} matching submissions for {} week {}", matchCount, raidType, weekId);

        // Check for lock (10+ matching submissions)
        if (matchCount >= 10) {
            logger.info("Locking loot pool for {} week {} with {} submissions", raidType, weekId, matchCount);

            Optional<RaidLootPoolApproved> existing = approvedRepo.findByRaidTypeAndWeekIdentifier(raidType, weekId);
            if (existing.isPresent()) {
                RaidLootPoolApproved approved = existing.get();
                approved.setLocked(true);
                approved.setSubmissionCount(matchCount);
                approvedRepo.save(approved);
            } else {
                RaidLootPoolApproved approved = new RaidLootPoolApproved(raidType, aspectsJson, weekId, matchCount, true);
                approvedRepo.save(approved);
            }
            return deserializeAspects(aspectsJson);
        }

        // Check for approval (3+ matching submissions)
        if (matchCount >= 3) {
            logger.info("Approving loot pool for {} week {} with {} submissions", raidType, weekId, matchCount);

            Optional<RaidLootPoolApproved> existing = approvedRepo.findByRaidTypeAndWeekIdentifier(raidType, weekId);
            if (existing.isPresent()) {
                RaidLootPoolApproved approved = existing.get();
                approved.setAspectsJson(aspectsJson);
                approved.setSubmissionCount(matchCount);
                approvedRepo.save(approved);
            } else {
                RaidLootPoolApproved approved = new RaidLootPoolApproved(raidType, aspectsJson, weekId, matchCount, false);
                approvedRepo.save(approved);
            }
            return deserializeAspects(aspectsJson);
        }

        logger.info("Not enough matching submissions yet ({}/3) for {} week {}", matchCount, raidType, weekId);
        return null;
    }

    private String serializeAspects(List<LootPoolSubmissionDto.AspectDto> aspects) {
        try {
            return objectMapper.writeValueAsString(aspects);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize aspects", e);
        }
    }

    private LootPoolSubmissionDto deserializeAspects(String json) {
        try {
            List<LootPoolSubmissionDto.AspectDto> aspects = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, LootPoolSubmissionDto.AspectDto.class)
            );
            return new LootPoolSubmissionDto(aspects);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize aspects", e);
        }
    }
}
