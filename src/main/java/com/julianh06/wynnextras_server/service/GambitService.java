package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianh06.wynnextras_server.dto.GambitSubmissionDto;
import com.julianh06.wynnextras_server.entity.GambitApproved;
import com.julianh06.wynnextras_server.entity.GambitSubmission;
import com.julianh06.wynnextras_server.repository.GambitApprovedRepository;
import com.julianh06.wynnextras_server.repository.GambitSubmissionRepository;
import com.julianh06.wynnextras_server.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GambitService {
    private static final Logger logger = LoggerFactory.getLogger(GambitService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private GambitSubmissionRepository submissionRepo;

    @Autowired
    private GambitApprovedRepository approvedRepo;

    /**
     * Submit gambits for the day
     * Returns the approved gambits if submission triggers approval, null otherwise
     */
    @Transactional
    public GambitSubmissionDto submitGambits(List<GambitSubmissionDto.GambitDto> gambits, String username) {
        String dayId = TimeUtils.getDayIdentifier();

        // Check if already approved and locked for today
        Optional<GambitApproved> existing = approvedRepo.findByDayIdentifier(dayId);
        if (existing.isPresent() && existing.get().isLocked()) {
            logger.info("Gambits for day {} are locked, ignoring submission from {}", dayId, username);
            return deserializeGambits(existing.get().getGambitsJson());
        }

        // Sort gambits by name for comparison
        List<GambitSubmissionDto.GambitDto> sortedGambits = gambits.stream()
            .sorted(Comparator.comparing(GambitSubmissionDto.GambitDto::getName))
            .collect(Collectors.toList());

        String gambitsJson = serializeGambits(sortedGambits);

        // Save submission
        GambitSubmission submission = new GambitSubmission(gambitsJson, username, dayId);
        submissionRepo.save(submission);
        logger.info("Saved gambit submission for day {} from {}", dayId, username);

        // Check if should approve
        return checkAndApprove(dayId, gambitsJson);
    }

    /**
     * Get the approved gambits for today
     */
    public GambitSubmissionDto getApprovedGambits() {
        String dayId = TimeUtils.getDayIdentifier();
        Optional<GambitApproved> approved = approvedRepo.findByDayIdentifier(dayId);

        if (approved.isPresent()) {
            return deserializeGambits(approved.get().getGambitsJson());
        }

        return null;
    }

    /**
     * Check if this submission should trigger approval
     * Returns the approved gambits if approved, null otherwise
     */
    @Transactional
    private GambitSubmissionDto checkAndApprove(String dayId, String gambitsJson) {
        // Get all submissions for today with matching JSON
        List<GambitSubmission> allSubmissions = submissionRepo.findByDayIdentifier(dayId);
        List<GambitSubmission> matchingSubmissions = allSubmissions.stream()
            .filter(s -> s.getGambitsJson().equals(gambitsJson))
            .collect(Collectors.toList());

        int matchCount = matchingSubmissions.size();
        logger.info("Found {} matching gambit submissions for day {}", matchCount, dayId);

        // Require 2+ matching submissions for approval and lock
        if (matchCount >= 2) {
            logger.info("Approving and locking gambits for day {} with {} submissions", dayId, matchCount);

            Optional<GambitApproved> existing = approvedRepo.findByDayIdentifier(dayId);
            if (existing.isPresent()) {
                GambitApproved approved = existing.get();
                approved.setGambitsJson(gambitsJson);
                approved.setLocked(true);
                approvedRepo.save(approved);
            } else {
                GambitApproved approved = new GambitApproved(gambitsJson, dayId, true);
                approvedRepo.save(approved);
            }
            return deserializeGambits(gambitsJson);
        }

        logger.info("Not enough matching submissions yet ({}/2) for day {}", matchCount, dayId);
        return null;
    }

    private String serializeGambits(List<GambitSubmissionDto.GambitDto> gambits) {
        try {
            return objectMapper.writeValueAsString(gambits);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize gambits", e);
        }
    }

    private GambitSubmissionDto deserializeGambits(String json) {
        try {
            List<GambitSubmissionDto.GambitDto> gambits = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, GambitSubmissionDto.GambitDto.class)
            );
            return new GambitSubmissionDto(gambits);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize gambits", e);
        }
    }
}
