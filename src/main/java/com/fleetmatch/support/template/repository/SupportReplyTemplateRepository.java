package com.fleetmatch.support.template.repository;

import com.fleetmatch.support.template.entity.SupportReplyTemplate;
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
