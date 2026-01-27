package com.julianh06.wynnextras_server.repository;
import com.julianh06.wynnextras_server.entity.RaidLootPoolSubmission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RaidLootPoolSubmissionRepository extends JpaRepository<RaidLootPoolSubmission, Long> {
    List<RaidLootPoolSubmission> findByRaidTypeAndWeekIdentifier(String raidType, String weekIdentifier);
    List<RaidLootPoolSubmission> findByWeekIdentifier(String weekIdentifier);
    List<RaidLootPoolSubmission> findByRaidTypeAndWeekIdentifierAndSubmittedByOrderBySubmittedAtDesc(String raidType, String weekIdentifier, String submittedBy);
}
