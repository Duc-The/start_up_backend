package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.PoiReview;

public interface PoiReviewManagementRepository extends ManagementRepository<PoiReview> {
  interface RatingSummary {
    double getAverage();

    long getTotal();
  }

  @org.springframework.data.jpa.repository.Query(
      "select coalesce(avg(r.rating),0) as average, count(r) as total from PoiReview r where"
          + " r.poi.id=:id and"
          + " r.status=ocop.vn.ocop_tourims_platform.entity.ReviewStatus.PUBLISHED")
  RatingSummary ratingSummary(
      @org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
