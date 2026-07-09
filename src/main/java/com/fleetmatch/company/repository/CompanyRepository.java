package com.fleetmatch.company.repository;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    long countByType(CompanyType type);
    long countByVerificationStatus(
            CompanyVerificationStatus verificationStatus
    );

    Optional<Company> findById(UUID id);

    List<Company> findByVerificationStatus(
            CompanyVerificationStatus verificationStatus
    );

    List<Company> findByVerificationStatusIn(
            Collection<CompanyVerificationStatus> verificationStatuses
    );

    Page<Company> findByVerificationStatusIn(
            Collection<CompanyVerificationStatus> verificationStatuses,
            Pageable pageable
    );

    Page<Company> findByVerificationStatusInAndType(
            Collection<CompanyVerificationStatus> verificationStatuses,
            CompanyType type,
            Pageable pageable
    );

    List<Company> findTop25ByHeadquartersIsNotNullAndHeadquartersAddressVerificationStatus(
            String headquartersAddressVerificationStatus
    );

    long countByVerificationStatusIn(
            Collection<CompanyVerificationStatus> verificationStatuses
    );

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.mcNumber) = lower(:value)
            """)
    long countDuplicateMcNumber(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.dotNumber) = lower(:value)
            """)
    long countDuplicateDotNumber(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.ein) = lower(:value)
            """)
    long countDuplicateEin(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.email) = lower(:value)
            """)
    long countDuplicateEmail(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and company.phone = :value
            """)
    long countDuplicatePhone(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.website) = lower(:value)
            """)
    long countDuplicateWebsite(@Param("companyId") UUID companyId, @Param("value") String value);

    @Query("""
            select count(company)
            from Company company
            where company.id <> :companyId
              and lower(company.headquarters) = lower(:value)
            """)
    long countDuplicateHeadquarters(@Param("companyId") UUID companyId, @Param("value") String value);
}
