package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {}
