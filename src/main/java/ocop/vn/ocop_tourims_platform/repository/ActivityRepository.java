package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {}
