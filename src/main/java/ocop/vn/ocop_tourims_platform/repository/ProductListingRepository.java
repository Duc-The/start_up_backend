package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.ProductListing;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductListingRepository extends JpaRepository<ProductListing, UUID> {
  @Query(
      "select l from ProductListing l join l.product p left join p.business b where l.poi.id=:poiId"
          + " and l.active=true and p.active=true and (b is null or b.active=true)")
  Page<ProductListing> findByPoiIdAndActiveTrueAndProductActiveTrue(
      @org.springframework.data.repository.query.Param("poiId") UUID poiId, Pageable pageable);
}
