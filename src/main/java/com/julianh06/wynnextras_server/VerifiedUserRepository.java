package com.julianh06.wynnextras_server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VerifiedUserRepository extends JpaRepository<VerifiedUser, Long> {
    Optional<VerifiedUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
