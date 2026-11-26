package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.CatalogForm;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.PageData;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper.Catalog;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogManagementService {
  private final AreaManagementRepository areas;
  private final PoiCategoryManagementRepository categories;
  private final ProductTypeManagementRepository types;
  private final ActivityManagementRepository activities;
  private final AmenityManagementRepository amenities;
  private final ManagementAccess access;
  private final ManagementChanges changes;

  private record CatalogStore<T extends BaseEntity>(
      ManagementRepository<T> repository, java.util.function.Supplier<T> factory) {}

  private CatalogStore<?> store(String type) {
    return switch (type) {
      case "areas" -> new CatalogStore<>(areas, Area::new);
      case "poi-categories" -> new CatalogStore<>(categories, PoiCategory::new);
      case "product-types" -> new CatalogStore<>(types, ProductType::new);
      case "activities" -> new CatalogStore<>(activities, Activity::new);
      case "amenities" -> new CatalogStore<>(amenities, Amenity::new);
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    };
  }

  private Catalog view(BaseEntity e) {
    return switch (e) {
      case Area a ->
          new Catalog(a.getId(), a.getCode(), a.getName(), null, null, null, null, 0, a.isActive());
      case ProductType t ->
          new Catalog(
              t.getId(),
              t.getProductTypeId(),
              t.getName(),
              t.getSlug(),
              t.getIconUrl(),
              null,
              t.getParent() == null ? null : t.getParent().getId(),
              t.getSortOrder(),
              t.isActive());
      case PoiCategory c ->
          new Catalog(
              c.getId(),
              c.getCategoryId(),
              c.getName(),
              null,
              c.getIconUrl(),
              c.getColorHex(),
              c.getParent() == null ? null : c.getParent().getId(),
              0,
              true);
      case Activity a ->
          new Catalog(
              a.getId(),
              a.getCode(),
              a.getName(),
              null,
              a.getIconUrl(),
              null,
              null,
              a.getSortOrder(),
              a.isActive());
      case Amenity a ->
          new Catalog(
              a.getId(),
              a.getCode(),
              a.getName(),
              null,
              a.getIconUrl(),
              null,
              null,
              a.getSortOrder(),
              a.isActive());
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    };
  }

  public PageData<Catalog> list(String type, String keyword, int page, int size) {
    access.current();
    return listTyped(store(type), keyword, page, size);
  }

  private <T extends BaseEntity> PageData<Catalog> listTyped(
      CatalogStore<T> store, String keyword, int page, int size) {
    Specification<T> spec =
        (r, q, b) ->
            keyword == null || keyword.isBlank()
                ? b.conjunction()
                : b.like(b.lower(r.get("name")), ManagementService.pattern(keyword), '!');
    var result = store.repository().findAll(spec, ManagementService.pageable(page, size));
    return new PageData<>(
        result.getContent().stream().map(this::view).toList(),
        result.getTotalElements(),
        page,
        size);
  }

  @Transactional
  public Catalog save(String type, UUID id, CatalogForm f) {
    access.requireAdmin();
    return saveTyped(store(type), type, id, f);
  }

  private <T extends BaseEntity> Catalog saveTyped(
      CatalogStore<T> store, String type, UUID id, CatalogForm f) {
    var repo = store.repository();
    T e =
        id == null
            ? store.factory().get()
            : repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    if (!type.equals("poi-categories")
        && !type.equals("product-types")
        && (f.code() == null || !f.code().matches("^[A-Z][A-Z0-9_]*$")))
      ManagementAccess.invalid(
          (type.equals("areas") ? "Mã khu vực" : "Mã danh mục")
              + " phải bắt đầu bằng chữ A–Z và chỉ gồm chữ viết hoa, số hoặc dấu gạch dưới (_)");
    switch (e) {
      case Area a -> {
        a.setName(f.name());
        a.setCode(f.code());
        a.setActive(f.active());
      }
      case Activity a -> {
        icon(f);
        a.setName(f.name());
        a.setCode(f.code());
        a.setIconUrl(f.iconUrl());
        a.setSortOrder(f.sortOrder());
        a.setActive(f.active());
      }
      case Amenity a -> {
        icon(f);
        a.setName(f.name());
        a.setCode(f.code());
        a.setIconUrl(f.iconUrl());
        a.setSortOrder(f.sortOrder());
        a.setActive(f.active());
      }
      case ProductType t -> {
        icon(f);
        if (f.slug() == null || !f.slug().matches("^[a-z0-9]+(-[a-z0-9]+)*$"))
          ManagementAccess.invalid("Slug không hợp lệ");
        t.setName(f.name());
        t.setSlug(f.slug());
        t.setIconUrl(f.iconUrl());
        t.setSortOrder(f.sortOrder());
        t.setActive(f.active());
        t.setParent(
            f.parentId() == null
                ? null
                : types
                    .findById(f.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
      }
      case PoiCategory c -> {
        c.setName(f.name());
        c.setIconUrl(f.iconUrl());
        c.setColorHex(f.colorHex());
        PoiCategory parent =
            f.parentId() == null
                ? null
                : categories
                    .findById(f.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Set<UUID> visited = new HashSet<>();
        for (PoiCategory p = parent; p != null; p = p.getParent()) {
          if (p == c || Objects.equals(p.getId(), c.getId()) || !visited.add(p.getId()))
            ManagementAccess.invalid("Danh mục cha không được tạo vòng lặp");
        }
        c.setParent(parent);
      }
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    access.stamp(e, id == null);
    repo.saveAndFlush(e);
    changes.record(type, e, null, id == null ? "CREATE" : "UPDATE", "");
    return view(e);
  }

  private void icon(CatalogForm f) {
    if (f.iconUrl() == null || f.iconUrl().isBlank())
      ManagementAccess.invalid("Biểu tượng không được trống");
  }

  @Transactional
  public void delete(String type, UUID id) {
    access.requireAdmin();
    deleteTyped(store(type), type, id);
  }

  private <T extends BaseEntity> void deleteTyped(CatalogStore<T> store, String type, UUID id) {
    var repo = store.repository();
    T e = repo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    changes.record(type, e, null, "DELETE", "");
    repo.delete(e);
    repo.flush();
  }
}
