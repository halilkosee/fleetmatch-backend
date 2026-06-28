package com.fleetmatch.company.review.service;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.review.dto.CompanyReviewEventResponse;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.entity.CompanyReviewEvent;
import com.fleetmatch.company.review.repository.CompanyReviewEventRepository;
import com.fleetmatch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyReviewEventService {

    private final CompanyReviewEventRepository companyReviewEventRepository;

    public void record(
            Company company,
            User actorUser,
            CompanyReviewAction action,
            UUID relatedDocumentId,
            String reason,
            String notes
    ) {
        CompanyReviewEvent event = new CompanyReviewEvent();
        event.setCompany(company);
        event.setActorUser(actorUser);
        event.setAction(action);
        event.setRelatedDocumentId(relatedDocumentId);
        event.setReason(reason);
        event.setNotes(notes);
        companyReviewEventRepository.save(event);
    }

    public List<CompanyReviewEventResponse> getEvents(UUID companyId) {
        return companyReviewEventRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(event -> new CompanyReviewEventResponse(
                        event.getId(),
                        event.getAction(),
                        event.getActorUser() == null ? null : event.getActorUser().getId(),
                        event.getActorUser() == null
                                ? null
                                : event.getActorUser().getFirstName() + " " + event.getActorUser().getLastName(),
                        event.getRelatedDocumentId(),
                        event.getReason(),
                        event.getNotes(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
