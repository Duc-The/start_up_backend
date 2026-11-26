package ocop.vn.ocop_tourims_platform.repository;

import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, UUID> {
  @org.springframework.data.jpa.repository.Query(
      value =
          "WITH RECURSIVE tree AS (SELECT id FROM product_types WHERE id IN (:ids) UNION SELECT"
              + " c.id FROM product_types c JOIN tree t ON c.parent_ref_id=t.id) SELECT id FROM"
              + " tree",
      nativeQuery = true)
  java.util.List<UUID> descendants(
      @org.springframework.data.repository.query.Param("ids") java.util.Collection<UUID> ids);
}
