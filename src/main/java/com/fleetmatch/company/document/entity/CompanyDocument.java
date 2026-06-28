package com.fleetmatch.company.document.entity;

import com.fleetmatch.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_documents")
@Getter
@Setter
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    private String storageKey;

    private String originalFileName;

    private String contentType;

    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING;

    @Column(length = 2000)
    private String reviewNotes;

    private LocalDateTime reviewedAt;

    private UUID reviewedByUserId;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}
