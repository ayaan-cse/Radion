package com.radion.repository;

import com.radion.domain.models.CompanyTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyTimelineRepository extends JpaRepository<CompanyTimeline, UUID> {
    Optional<CompanyTimeline> findByUserIdAndCompanyNameIgnoreCase(UUID userId, String companyName);
    List<CompanyTimeline> findByUserIdOrderByLastUpdatedDesc(UUID userId);
    List<CompanyTimeline> findByUserId(UUID userId);
}
