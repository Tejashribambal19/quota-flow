package com.tejashri.quota.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejashri.quota.domain.AppUser;
import com.tejashri.quota.domain.UserRole;

public interface AppUserRepository
        extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);
}