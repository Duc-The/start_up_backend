package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, UUID> {}
