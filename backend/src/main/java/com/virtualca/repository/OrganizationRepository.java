package com.virtualca.repository;

import com.virtualca.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByGstin(String gstin);
    List<Organization> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Organization> findAllByOrderByCreatedAtDesc();
}

