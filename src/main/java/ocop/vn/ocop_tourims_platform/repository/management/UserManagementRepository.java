package ocop.vn.ocop_tourims_platform.repository.management;

import ocop.vn.ocop_tourims_platform.entity.User;

public interface UserManagementRepository extends ManagementRepository<User> {
  @org.springframework.data.jpa.repository.Query(
      value = "select 1 from pg_advisory_xact_lock(17001118)",
      nativeQuery = true)
  Integer lockAdministration();
}
