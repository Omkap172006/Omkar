package com.virtualca.repository;

import com.virtualca.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByOrgId(UUID orgId);
    Optional<Account> findByOrgIdAndAccountCode(UUID orgId, String accountCode);
}

