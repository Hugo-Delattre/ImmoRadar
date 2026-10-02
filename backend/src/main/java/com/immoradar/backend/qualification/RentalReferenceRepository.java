package com.immoradar.backend.qualification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RentalReferenceRepository extends JpaRepository<RentalReference, String> {
    List<RentalReference> findAllByDealIdOrderByRecordedAtDesc(String dealId);
    Optional<RentalReference> findByIdAndDealId(String id, String dealId);
}
