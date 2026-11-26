package ocop.vn.ocop_tourims_platform.repository;

import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByUserId(UUID userId, Pageable pageable);

  Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

  long countByUserIdAndReadFalse(UUID userId);

  Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

  @Modifying
  @Query(
      "update Notification n set n.read=true, n.updatedAt=CURRENT_TIMESTAMP where n.user.id=:user"
          + " and n.read=false")
  void readAll(@Param("user") UUID user);
}
