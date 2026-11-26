package ocop.vn.ocop_tourims_platform.repository;

import jakarta.persistence.*;
import ocop.vn.ocop_tourims_platform.common.SearchText;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.dto.request.PoiFilter;
import ocop.vn.ocop_tourims_platform.entity.Poi;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomeQueryRepository {
  private final EntityManager em;

  public record Result(List<Poi> items, long total) {}

  private final PoiRepository pois;
  private final ProductTypeRepository productTypes;
  private final PoiCategoryRepository categories;

  public Result search(PoiFilter filter, boolean map) {
    Collection<UUID> typeIds =
        filter.productTypeIds() == null || filter.productTypeIds().isEmpty()
            ? null
            : productTypes.descendants(filter.productTypeIds());
    Collection<UUID> categoryIds =
        filter.categoryIds() == null || filter.categoryIds().isEmpty()
            ? List.of()
            : categories.descendants(filter.categoryIds());
    var spec =
        ocop.vn.ocop_tourims_platform.repository.specification.PoiSpecification.filter(
            filter, typeIds, categoryIds, map);
    int size = map ? 2000 : filter.pageSize();
    var page =
        pois.findAll(
            spec,
            org.springframework.data.domain.PageRequest.of(map ? 0 : filter.pageNumber(), size));
    List<Poi> items = page.getContent();
    // Fetch collections after SQL pagination, separately to avoid cartesian multiplication.
    for (int offset = 0; offset < items.size(); offset += 100) {
      var ids =
          items.subList(offset, Math.min(items.size(), offset + 100)).stream()
              .map(Poi::getId)
              .toList();
      pois.loadCategories(ids);
      if (!map) pois.loadMedia(ids);
    }
    return new Result(items, page.getTotalElements());
  }

  public List<ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.Suggestion> suggestions(
      String keyword, int limit) {
    String normalizedName = "translate(lower(name), '" + SearchText.ACCENTS + "', '" + SearchText.PLAIN + "')";
    String normalizedKeyword = SearchText.normalize(keyword);
    String pattern = SearchText.pattern(keyword);
    Query q =
        em.createNativeQuery(
            "SELECT id, kind, name FROM (SELECT id,'POI' AS kind,name FROM pois WHERE"
                + " is_active=true AND (business_ref_id is null or business_ref_id in (select id"
                + " from businesses where active=true)) AND " + normalizedName + " LIKE :keyword ESCAPE '!'"
                + " UNION ALL SELECT id,'PRODUCT' AS kind,name FROM products WHERE is_active=true"
                + " AND (business_ref_id is null or business_ref_id in (select id from businesses"
                + " where active=true)) AND " + normalizedName + " LIKE :keyword ESCAPE '!') found ORDER BY"
                + " case when kind='POI' and " + normalizedName + " = :exact then 0 when kind='POI' then 1 else 2 end, name,id");
    q.setParameter("keyword", pattern).setParameter("exact", normalizedKeyword).setMaxResults(limit);
    @SuppressWarnings("unchecked")
    List<Object[]> rows = q.getResultList();
    return rows.stream()
        .map(
            row ->
                new ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.Suggestion(
                    (UUID) row[0], (String) row[1], (String) row[2]))
        .toList();
  }
}
