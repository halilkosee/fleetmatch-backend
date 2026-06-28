package com.fleetmatch.support.repository;

import com.fleetmatch.support.entity.SupportReplyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportReplyTemplateRepository
        extends JpaRepository<SupportReplyTemplate, UUID> {

    boolean existsByTemplateKey(String templateKey);

    Optional<SupportReplyTemplate> findByTemplateKey(String templateKey);

    List<SupportReplyTemplate> findByActiveTrueOrderByTitleAsc();
}
