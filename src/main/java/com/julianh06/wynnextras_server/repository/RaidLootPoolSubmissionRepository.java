package com.julianh06.wynnextras_server.repository;
nimport com.julianh06.wynnextras_server.entity.RaidLootPoolSubmission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RaidLootPoolSubmissionRepository extends JpaRepository<RaidLootPoolSubmission, Long> {
    List<RaidLootPoolSubmission> findByRaidTypeAndWeekIdentifier(String raidType, String weekIdentifier);
    List<RaidLootPoolSubmission> findByWeekIdentifier(String weekIdentifier);
}
