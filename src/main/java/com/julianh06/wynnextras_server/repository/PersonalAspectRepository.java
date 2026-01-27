package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.PersonalAspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalAspectRepository extends JpaRepository<PersonalAspect, Long> {
    List<PersonalAspect> findByPlayerUuid(String playerUuid);
    Optional<PersonalAspect> findByPlayerUuidAndAspectName(String playerUuid, String aspectName);
    void deleteByPlayerUuid(String playerUuid);
}
