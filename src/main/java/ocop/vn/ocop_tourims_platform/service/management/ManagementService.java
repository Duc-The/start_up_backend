package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.PageData;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagementService {
  private final ManagementStore store;
  private final ManagementAccess access;
  private final ManagementMapper mapper;
  private final ManagementChanges changes;

  public PageData<Item> list(
      String type, String keyword, UUID businessId, PublicationStatus status, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> owned = admin ? Set.of() : access.businessIds();
    if (businessId != null) access.business(businessId);
    return listTyped(store.repository(type), keyword, businessId, status, admin, owned, page, size);
  }

  private <T extends ManagedContent> PageData<Item> listTyped(
      ocop.vn.ocop_tourims_platform.repository.management.ManagementRepository<T> repo,
      String keyword,
      UUID businessId,
      PublicationStatus status,
      boolean admin,
      Set<UUID> owned,
      int page,
      int size) {
    Specification<T> spec =
        (root, q, cb) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          if (!admin) p.add(root.get("business").get("id").in(owned));
          if (businessId != null) p.add(cb.equal(root.get("business").get("id"), businessId));
          if (status != null) p.add(cb.equal(root.get("publicationStatus"), status));
          if (keyword != null && !keyword.isBlank())
            p.add(cb.like(cb.lower(root.get("name")), pattern(keyword), '!'));
          return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Page<T> data = repo.findAll(spec, pageable(page, size));
    return new PageData<>(
        data.getContent().stream().map(mapper::item).toList(), data.getTotalElements(), page, size);
  }

  public Item detail(String type, UUID id) {
    var c = store.content(type, id);
    access.content(c);
    return mapper.item(c);
  }

  @Transactional
  public Item workflow(String type, UUID id, String action, long version, String reason) {
    var c = store.content(type, id);
    access.content(c);
    ManagementAccess.version(c, version);
    switch (action) {
      case "submit" -> {
        if (c.getPublicationStatus() != PublicationStatus.DRAFT
            && c.getPublicationStatus() != PublicationStatus.REJECTED)
          ManagementAccess.invalid("Chỉ bản nháp hoặc nội dung bị từ chối được gửi duyệt");
        if (type.equals("tours") && ((Tour) c).getStops().isEmpty())
          ManagementAccess.invalid("Tour cần ít nhất một điểm dừng");
        c.setPublicationStatus(PublicationStatus.PENDING_REVIEW);
        c.setRejectionReason("");
      }
      case "approve", "reject" -> {
        access.requireAdmin();
        if (c.getPublicationStatus() != PublicationStatus.PENDING_REVIEW)
          ManagementAccess.invalid("Nội dung chưa ở trạng thái chờ duyệt");
        if (c.getBusiness() != null && !c.getBusiness().isActive())
          ManagementAccess.invalid("Cơ sở đã ngừng hoạt động");
        if (action.equals("reject") && (reason == null || reason.isBlank()))
          ManagementAccess.invalid("Cần cung cấp lý do từ chối");
        c.setPublicationStatus(
            action.equals("approve") ? PublicationStatus.PUBLISHED : PublicationStatus.REJECTED);
        c.setRejectionReason(action.equals("reject") ? reason : "");
      }
      case "archive" -> c.setPublicationStatus(PublicationStatus.ARCHIVED);
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    c.setLastManagementChange(java.time.Instant.now());
    visibility(c);
    changes.record(
        type, c, ManagementMapper.id(c.getBusiness()), action, reason == null ? "" : reason);
    // Flush optimistic version before returning the next version to the client.
    store.repository(type).flush();
    return mapper.item(c);
  }

  public void draft(ManagedContent c, boolean created, Long version) {
    access.content(c);
    if (!created) ManagementAccess.version(c, version);
    if (c.getPublicationStatus() == PublicationStatus.ARCHIVED && !created)
      ManagementAccess.invalid("Nội dung đã lưu trữ không được sửa");
    c.setPublicationStatus(PublicationStatus.DRAFT);
    c.setRejectionReason("");
    visibility(c);
    access.stamp(c, created);
    c.setLastManagementChange(java.time.Instant.now());
  }

  private void visibility(ManagedContent c) {
    boolean published = c.getPublicationStatus() == PublicationStatus.PUBLISHED;
    if (c instanceof Poi p) {
      p.setActive(published);
      p.setVerified(published);
    }
    if (c instanceof Product p) p.setActive(published);
    if (c instanceof Tour t)
      t.setStatus(
          published
              ? Tour.Status.PUBLISHED
              : c.getPublicationStatus() == PublicationStatus.ARCHIVED
                  ? Tour.Status.ARCHIVED
                  : Tour.Status.DRAFT);
  }

  public static Pageable pageable(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      ManagementAccess.invalid("Trang phải từ 0, kích thước trang từ 1 đến 100");
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
  }

  public static String pattern(String s) {
    return "%"
        + s.strip()
            .toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
        + "%";
  }
}
