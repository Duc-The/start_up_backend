package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.Poi;

public interface PoiManagementRepository extends ManagementRepository<Poi> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select p from Poi p where p.id=:id")
  java.util.Optional<Poi> findForUpdate(
      @org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
