package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoiReviewRepository extends JpaRepository<PoiReview, UUID> {
  Page<PoiReview> findByPoiIdAndStatus(UUID poiId, ReviewStatus status, Pageable pageable);
}
