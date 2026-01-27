package com.julianh06.wynnextras_server.repository;
nimport com.julianh06.wynnextras_server.entity.GambitApproved;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GambitApprovedRepository extends JpaRepository<GambitApproved, Long> {
    Optional<GambitApproved> findByDayIdentifier(String dayIdentifier);
}
