package com.julianh06.wynnextras_server.repository;
nimport com.julianh06.wynnextras_server.entity.GambitSubmission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GambitSubmissionRepository extends JpaRepository<GambitSubmission, Long> {
    List<GambitSubmission> findByDayIdentifier(String dayIdentifier);
}
