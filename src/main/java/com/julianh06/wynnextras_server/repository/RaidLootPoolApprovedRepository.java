package com.julianh06.wynnextras_server.repository;
nimport com.julianh06.wynnextras_server.entity.RaidLootPoolApproved;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RaidLootPoolApprovedRepository extends JpaRepository<RaidLootPoolApproved, Long> {
    Optional<RaidLootPoolApproved> findByRaidTypeAndWeekIdentifier(String raidType, String weekIdentifier);
}
