package com.fleetmatch.email.repository;

import com.fleetmatch.email.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    Optional<EmailTemplate> findByTemplateKeyAndActiveTrue(String templateKey);

    boolean existsByTemplateKey(String templateKey);
}
