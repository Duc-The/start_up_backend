package ocop.vn.ocop_tourims_platform.repository;

import java.util.Optional;
import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.Role;
import org.springframework.data.jpa.repository.*;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  @Modifying
  @Query(
      value =
          "INSERT INTO roles (id,code,name,description,created_at,updated_at) VALUES"
              + " (gen_random_uuid(),:code,:name,:description,now(),now()) ON CONFLICT (code) DO"
              + " NOTHING",
      nativeQuery = true)
  void ensureRole(
      @org.springframework.data.repository.query.Param("code") String code,
      @org.springframework.data.repository.query.Param("name") String name,
      @org.springframework.data.repository.query.Param("description") String description);

  Optional<Role> findByCode(String code);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO roles (id, code, name, description, created_at, updated_at)
          VALUES (gen_random_uuid(), 'CUSTOMER', 'Khách hàng', 'Tài khoản mua hàng và khám phá tour', now(), now())
          ON CONFLICT (code) DO NOTHING
          """,
      nativeQuery = true)
  void ensureCustomerRole();
}
