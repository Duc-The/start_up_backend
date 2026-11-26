package ocop.vn.ocop_tourims_platform.repository;

import java.util.*;
import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.Poi;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoiRepository extends JpaRepository<Poi, UUID>, JpaSpecificationExecutor<Poi> {
  @Override
  @EntityGraph(attributePaths = {"area", "geometry"})
  Page<Poi> findAll(Specification<Poi> specification, Pageable pageable);

  @Query(
      "select distinct p from Poi p left join fetch p.categoryLinks c left join fetch c.category"
          + " where p.id in :ids")
  List<Poi> loadCategories(
      @org.springframework.data.repository.query.Param("ids") Collection<UUID> ids);

  @Query("select distinct p from Poi p left join fetch p.media where p.id in :ids")
  List<Poi> loadMedia(@org.springframework.data.repository.query.Param("ids") Collection<UUID> ids);
}
