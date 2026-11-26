package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.MediaAsset;

public interface MediaAssetManagementRepository extends ManagementRepository<MediaAsset> {
  @Override
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<MediaAsset> findById(java.util.UUID id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select a from MediaAsset a where a.id in :ids order by a.id")
  java.util.List<MediaAsset> lockAll(
      @org.springframework.data.repository.query.Param("ids")
          java.util.Collection<java.util.UUID> ids);
}
