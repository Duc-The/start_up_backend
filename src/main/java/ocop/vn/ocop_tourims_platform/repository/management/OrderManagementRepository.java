package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.Order;

public interface OrderManagementRepository extends ManagementRepository<Order> {
  @Override
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<Order> findById(java.util.UUID id);
}
