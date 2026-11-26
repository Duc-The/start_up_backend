package ocop.vn.ocop_tourims_platform.repository.management;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ManagementRepository<T>
    extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {}
