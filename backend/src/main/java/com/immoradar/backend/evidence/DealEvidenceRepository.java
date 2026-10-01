package com.immoradar.backend.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DealEvidenceRepository extends JpaRepository<DealEvidence, String> {
    List<DealEvidence> findAllByDealId(String dealId);
}
