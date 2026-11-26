package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.ImportBatch;

public interface ImportBatchManagementRepository extends ManagementRepository<ImportBatch> {
  @Override
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<ImportBatch> findById(java.util.UUID id);
}
