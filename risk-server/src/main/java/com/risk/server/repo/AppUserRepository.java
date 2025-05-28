package com.risk.server.repo;

import com.risk.server.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser,Long>{
    Optional<AppUser> findByUsername(String u);
    boolean existsByUsername(String u);
}
