package ocop.vn.ocop_tourims_platform.service.management;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import ocop.vn.ocop_tourims_platform.repository.RoleRepository;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsManagementService {
  private final ManagementAccess access;
  private final ocop.vn.ocop_tourims_platform.service.JWTService jwt;
  private final ManagementMapper mapper;
  private final ManagementChanges changes;
  private final ManagementStore store;
  private final UserManagementRepository users;
  private final RoleRepository roles;
  private final OrderManagementRepository orders;
  private final PaymentManagementRepository payments;
  private final PoiReviewManagementRepository poiReviews;
  private final ProductReviewManagementRepository productReviews;
  private final ProductListingManagementRepository listings;
  private final CertificationManagementRepository certificates;
  private final PoiManagementRepository pois;
  private final ManagementAuditManagementRepository audits;

  public Dashboard dashboard() {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String type :
        List.of("pois", "products", "certifications", "stories", "events", "tours")) {
      counts.put(type, count(store.repository(type), admin, ids, false));
      counts.put(type + "Pending", count(store.repository(type), admin, ids, true));
    }
    counts.put("orders", orders.count(orderScope(admin, ids)));
    counts.put(
        "expiringCertifications",
        certificates.count(
            (r, q, b) ->
                b.and(
                    admin ? b.conjunction() : r.get("business").get("id").in(ids),
                    b.equal(r.get("publicationStatus"), PublicationStatus.PUBLISHED),
                    b.between(r.get("expiresOn"), LocalDate.now(), LocalDate.now().plusDays(30)))));
    return new Dashboard(counts);
  }

  private <T extends ManagedContent> long count(
      ManagementRepository<T> repo, boolean admin, Set<UUID> ids, boolean pending) {
    return repo.count(
        (r, q, b) ->
            b.and(
                admin ? b.conjunction() : r.get("business").get("id").in(ids),
                pending
                    ? b.equal(r.get("publicationStatus"), PublicationStatus.PENDING_REVIEW)
                    : b.conjunction()));
  }

  public PageData<ManagementMapper.UserView> users(String keyword, int page, int size) {
    access.requireAdmin();
    var result =
        users.findAll(
            (r, q, b) ->
                keyword == null || keyword.isBlank()
                    ? b.conjunction()
                    : b.or(
                        b.like(b.lower(r.get("email")), ManagementService.pattern(keyword), '!'),
                        b.like(
                            b.lower(r.get("displayName")),
                            ManagementService.pattern(keyword),
                            '!')),
            ManagementService.pageable(page, size));
    return page(result, mapper::user, page, size);
  }

  @Transactional
  public ManagementMapper.UserView roles(UUID id, Roles f) {
    access.requireAdmin();
    users.lockAdministration();
    User u = users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    protectAdmin(u, !f.roles().contains("ADMIN"));
    for (var link : new ArrayList<>(u.getUserRoles()))
      if (!f.roles().contains(link.getRole().getCode())) u.removeRole(link);
    for (String code : f.roles())
      if (u.getUserRoles().stream().noneMatch(link -> link.getRole().getCode().equals(code)))
        u.addRole(
            roles
                .findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)),
            access.current());
    users.saveAndFlush(u);
    changes.record("users", u, null, "ROLES_UPDATE", String.join(",", new TreeSet<>(f.roles())));
    return mapper.user(u);
  }

  @Transactional
  public ManagementMapper.UserView userStatus(UUID id, UserStatus f) {
    access.requireAdmin();
    users.lockAdministration();
    User u = users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    protectAdmin(u, f.status() != User.Status.ACTIVE);
    u.setStatus(f.status());
    users.flush();
    if (f.status() != User.Status.ACTIVE) jwt.revokeSessions(id);
    changes.record("users", u, null, "STATUS_UPDATE", f.status().name());
    return mapper.user(u);
  }

  private void protectAdmin(User u, boolean removing) {
    if (!removing) return;
    if (u.getId().equals(access.actor()))
      ManagementAccess.invalid(
          "Không thể tự thu hồi quyền quản trị hoặc khóa tài khoản đang sử dụng");
    boolean admin = u.getUserRoles().stream().anyMatch(r -> r.getRole().getCode().equals("ADMIN"));
    if (admin
        && users.count(
                (r, q, b) -> {
                  q.distinct(true);
                  return b.and(
                      b.equal(r.get("status"), User.Status.ACTIVE),
                      b.equal(r.join("userRoles").get("role").get("code"), "ADMIN"));
                })
            <= 1) ManagementAccess.invalid("Hệ thống phải còn ít nhất một quản trị viên hoạt động");
  }

  private Specification<Order> orderScope(boolean admin, Set<UUID> ids) {
    return (r, q, b) -> {
      if (admin) return b.conjunction();
      var sub = q.subquery(Integer.class);
      var item = sub.from(OrderItem.class);
      sub.select(b.literal(1))
          .where(
              b.equal(item.get("order"), r),
              item.get("productListing").get("poi").get("business").get("id").in(ids));
      return b.exists(sub);
    };
  }

  public record OrderLine(
      UUID id,
      UUID listingId,
      String productName,
      int quantity,
      long unitPriceVnd,
      OrderItem.FulfillmentStatus fulfillmentStatus) {}

  public record OrderView(
      UUID id,
      String orderId,
      Order.Status status,
      String recipientName,
      String recipientPhone,
      String deliveryAddress,
      List<OrderLine> items,
      long visibleSubtotalVnd,
      Long totalVnd) {}

  private OrderView orderView(Order o, boolean admin, Set<UUID> ids) {
    var items =
        o.getItems().stream()
            .filter(
                i ->
                    admin
                        || ids.contains(
                            ManagementMapper.id(i.getProductListing().getPoi().getBusiness())))
            .toList();
    return new OrderView(
        o.getId(),
        o.getOrderId(),
        o.getStatus(),
        o.getRecipientName(),
        o.getRecipientPhone(),
        o.getDeliveryAddress(),
        items.stream()
            .map(
                i ->
                    new OrderLine(
                        i.getId(),
                        i.getProductListing().getId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPriceVnd(),
                        i.getFulfillmentStatus()))
            .toList(),
        items.stream().mapToLong(OrderItem::getLineTotalVnd).sum(),
        admin ? o.getTotalVnd() : null);
  }

  public PageData<OrderView> orders(Order.Status status, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    var spec =
        orderScope(admin, ids)
            .and((r, q, b) -> status == null ? b.conjunction() : b.equal(r.get("status"), status));
    return page(
        orders.findAll(spec, ManagementService.pageable(page, size)),
        o -> orderView(o, admin, ids),
        page,
        size);
  }

  @Transactional
  public OrderView order(UUID id) {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    var o =
        orders.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    var result = orderView(o, admin, ids);
    if (!admin && result.items().isEmpty()) ManagementAccess.denied();
    return result;
  }

  @Transactional
  public OrderView fulfill(UUID orderId, UUID itemId, Fulfillment f) {
    Order o =
        orders
            .findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    var item =
        o.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    access.content(item.getProductListing().getPoi());
    if (o.getStatus() != Order.Status.PAID && o.getStatus() != Order.Status.SHIPPED)
      ManagementAccess.invalid("Chỉ được xử lý giao hàng sau khi thanh toán");
    var current = item.getFulfillmentStatus();
    if (current != f.status()
        && !((current == OrderItem.FulfillmentStatus.PENDING
                && f.status() == OrderItem.FulfillmentStatus.SHIPPED)
            || (current == OrderItem.FulfillmentStatus.SHIPPED
                && f.status() == OrderItem.FulfillmentStatus.COMPLETED)))
      ManagementAccess.invalid("Trạng thái giao hàng không hợp lệ");
    item.setFulfillmentStatus(f.status());
    access.stamp(item, false);
    if (o.getStatus() == Order.Status.PAID
        && o.getItems().stream()
            .allMatch(i -> i.getFulfillmentStatus() != OrderItem.FulfillmentStatus.PENDING))
      o.updateFulfillmentStatus(Order.Status.SHIPPED);
    if (o.getStatus() == Order.Status.SHIPPED
        && o.getItems().stream()
            .allMatch(i -> i.getFulfillmentStatus() == OrderItem.FulfillmentStatus.COMPLETED))
      o.updateFulfillmentStatus(Order.Status.COMPLETED);
    changes.record(
        "order-items",
        item,
        ManagementMapper.id(item.getProductListing().getPoi().getBusiness()),
        "FULFILLMENT",
        f.status().name());
    return orderView(o, access.admin(), access.businessIds());
  }

  @Transactional
  public OrderView orderStatus(UUID id, OrderStatus f) {
    access.requireAdmin();
    Order o =
        orders.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    // Management cannot mark a payment as paid or bypass payment confirmation.
    o.updateFulfillmentStatus(f.status());
    for (var item : o.getItems())
      item.setFulfillmentStatus(
          f.status() == Order.Status.SHIPPED
              ? OrderItem.FulfillmentStatus.SHIPPED
              : OrderItem.FulfillmentStatus.COMPLETED);
    changes.record("orders", o, null, "STATUS_UPDATE", f.status().name());
    return orderView(o, true, Set.of());
  }

  public PageData<ManagementMapper.PaymentView> payments(
      UUID orderId, Payment.Status status, int page, int size) {
    access.requireAdmin();
    return page(
        payments.findAll(
            (r, q, b) ->
                b.and(
                    orderId == null ? b.conjunction() : b.equal(r.get("order").get("id"), orderId),
                    status == null ? b.conjunction() : b.equal(r.get("status"), status)),
            ManagementService.pageable(page, size)),
        mapper::payment,
        page,
        size);
  }

  public PageData<Item> listings(UUID poiId, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    if (poiId != null)
      access.content(
          pois.findById(poiId)
              .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    return page(
        listings.findAll(
            (r, q, b) ->
                b.and(
                    admin ? b.conjunction() : r.get("poi").get("business").get("id").in(ids),
                    poiId == null ? b.conjunction() : b.equal(r.get("poi").get("id"), poiId)),
            ManagementService.pageable(page, size)),
        mapper::item,
        page,
        size);
  }

  @Transactional
  public void deactivateListing(UUID id) {
    var l =
        listings
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    access.content(l.getPoi());
    l.setActive(false);
    changes.record("listings", l, ManagementMapper.id(l.getPoi().getBusiness()), "DEACTIVATE", "");
  }

  public PageData<ManagementMapper.Review> reviews(
      String kind, ReviewStatus status, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    if (kind.equals("poi"))
      return page(
          poiReviews.findAll(
              (r, q, b) ->
                  b.and(
                      admin ? b.conjunction() : r.get("poi").get("business").get("id").in(ids),
                      status == null ? b.conjunction() : b.equal(r.get("status"), status)),
              ManagementService.pageable(page, size)),
          r ->
              new ManagementMapper.Review(
                  r.getId(),
                  r.getAuthor().getId(),
                  r.getPoi().getId(),
                  r.getRating(),
                  r.getComment(),
                  r.getStatus(),
                  r.getMedia().stream()
                      .map(
                          m ->
                              new Media(
                                  m.getId(),
                                  m.getMediaType().name(),
                                  m.getMediaUrl(),
                                  m.getSortOrder()))
                      .toList()),
          page,
          size);
    if (kind.equals("product"))
      return page(
          productReviews.findAll(
              (r, q, b) ->
                  b.and(
                      admin ? b.conjunction() : r.get("product").get("business").get("id").in(ids),
                      status == null ? b.conjunction() : b.equal(r.get("status"), status)),
              ManagementService.pageable(page, size)),
          r ->
              new ManagementMapper.Review(
                  r.getId(),
                  r.getAuthor().getId(),
                  r.getProduct().getId(),
                  r.getRating(),
                  r.getComment(),
                  r.getStatus(),
                  r.getMedia().stream()
                      .map(
                          m ->
                              new Media(
                                  m.getId(),
                                  m.getMediaType().name(),
                                  m.getMediaUrl(),
                                  m.getSortOrder()))
                      .toList()),
          page,
          size);
    throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Transactional
  public void moderate(String kind, UUID id, Moderation f) {
    access.requireAdmin();
    if (kind.equals("poi")) {
      var review =
          poiReviews
              .findById(id)
              .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
      Poi poi =
          pois.findForUpdate(review.getPoi().getId())
              .orElseThrow(); // Serialize summary updates for this POI.
      review.setStatus(f.status());
      poiReviews.flush();
      var summary = poiReviews.ratingSummary(poi.getId());
      poi.updateRatingSummary(
          java.math.BigDecimal.valueOf(summary.getAverage()), Math.toIntExact(summary.getTotal()));
      changes.record(
          "poi-reviews", review, ManagementMapper.id(poi.getBusiness()), "MODERATE", f.reason());
    } else if (kind.equals("product")) {
      var review =
          productReviews
              .findById(id)
              .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
      review.setStatus(f.status());
      changes.record(
          "product-reviews",
          review,
          ManagementMapper.id(review.getProduct().getBusiness()),
          "MODERATE",
          f.reason());
    } else throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
  }

  public record Audit(
      UUID id,
      String targetType,
      UUID targetId,
      UUID businessId,
      String action,
      String detail,
      UUID actorId,
      Instant createdAt) {}

  public PageData<Audit> audit(String type, UUID targetId, int page, int size) {
    access.requireAdmin();
    return page(
        audits.findAll(
            (r, q, b) ->
                b.and(
                    type == null ? b.conjunction() : b.equal(r.get("targetType"), type),
                    targetId == null ? b.conjunction() : b.equal(r.get("targetId"), targetId)),
            ManagementService.pageable(page, size)),
        a ->
            new Audit(
                a.getId(),
                a.getTargetType(),
                a.getTargetId(),
                a.getBusinessId(),
                a.getAction(),
                a.getDetail(),
                a.getCreatedBy(),
                a.getCreatedAt()),
        page,
        size);
  }

  private <T, R> PageData<R> page(
      Page<T> data, java.util.function.Function<T, R> mapper, int page, int size) {
    return new PageData<>(
        data.getContent().stream().map(mapper).toList(), data.getTotalElements(), page, size);
  }
}
