package ocop.vn.ocop_tourims_platform.controller.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.PageData;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import ocop.vn.ocop_tourims_platform.service.management.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/management")
public class AdministrationController {
  private final BusinessManagementService businesses;
  private final CatalogManagementService catalogs;
  private final OperationsManagementService operations;

  @GetMapping("/dashboard")
  public ResponseEntity<ApiResponse<Dashboard>> dashboard(HttpServletRequest r) {
    return ok(operations.dashboard(), r);
  }

  @GetMapping("/businesses")
  public ResponseEntity<ApiResponse<PageData<Item>>> businesses(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(businesses.list(keyword, page, size), r);
  }

  @GetMapping("/businesses/{id}")
  public ResponseEntity<ApiResponse<Item>> business(@PathVariable UUID id, HttpServletRequest r) {
    return ok(businesses.detail(id), r);
  }

  @PostMapping("/businesses")
  public ResponseEntity<ApiResponse<Item>> create(
      @Valid @RequestBody BusinessForm f, HttpServletRequest r) {
    return created(businesses.save(null, f), r);
  }

  @PutMapping("/businesses/{id}")
  public ResponseEntity<ApiResponse<Item>> update(
      @PathVariable UUID id, @Valid @RequestBody BusinessForm f, HttpServletRequest r) {
    return ok(businesses.save(id, f), r);
  }

  @GetMapping("/businesses/{id}/members")
  public ResponseEntity<ApiResponse<List<BusinessManagementService.Member>>> members(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(businesses.members(id), r);
  }

  @PutMapping("/businesses/{id}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> assign(
      @PathVariable UUID id, @PathVariable UUID userId, HttpServletRequest r) {
    businesses.assign(id, userId);
    return ok(null, r);
  }

  @DeleteMapping("/businesses/{id}/members/{userId}")
  public ResponseEntity<ApiResponse<Void>> unassign(
      @PathVariable UUID id, @PathVariable UUID userId, HttpServletRequest r) {
    businesses.unassign(id, userId);
    return ok(null, r);
  }

  @GetMapping("/catalogs/{type}")
  public ResponseEntity<ApiResponse<PageData<ManagementMapper.Catalog>>> catalogs(
      @PathVariable String type,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(catalogs.list(type, keyword, page, size), r);
  }

  @PostMapping("/catalogs/{type}")
  public ResponseEntity<ApiResponse<ManagementMapper.Catalog>> catalog(
      @PathVariable String type, @Valid @RequestBody CatalogForm f, HttpServletRequest r) {
    return created(catalogs.save(type, null, f), r);
  }

  @PutMapping("/catalogs/{type}/{id}")
  public ResponseEntity<ApiResponse<ManagementMapper.Catalog>> catalog(
      @PathVariable String type,
      @PathVariable UUID id,
      @Valid @RequestBody CatalogForm f,
      HttpServletRequest r) {
    return ok(catalogs.save(type, id, f), r);
  }

  @DeleteMapping("/catalogs/{type}/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCatalog(
      @PathVariable String type, @PathVariable UUID id, HttpServletRequest r) {
    catalogs.delete(type, id);
    return ok(null, r);
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<PageData<ManagementMapper.UserView>>> users(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.users(keyword, page, size), r);
  }

  @PutMapping("/users/{id}/roles")
  public ResponseEntity<ApiResponse<ManagementMapper.UserView>> roles(
      @PathVariable UUID id, @Valid @RequestBody Roles f, HttpServletRequest r) {
    return ok(operations.roles(id, f), r);
  }

  @PatchMapping("/users/{id}/status")
  public ResponseEntity<ApiResponse<ManagementMapper.UserView>> status(
      @PathVariable UUID id, @Valid @RequestBody UserStatus f, HttpServletRequest r) {
    return ok(operations.userStatus(id, f), r);
  }

  @GetMapping("/orders")
  public ResponseEntity<ApiResponse<PageData<OperationsManagementService.OrderView>>> orders(
      @RequestParam(required = false) Order.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.orders(status, page, size), r);
  }

  @GetMapping("/orders/{id}")
  public ResponseEntity<ApiResponse<OperationsManagementService.OrderView>> order(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(operations.order(id), r);
  }

  @PatchMapping("/orders/{id}/status")
  public ResponseEntity<ApiResponse<OperationsManagementService.OrderView>> orderStatus(
      @PathVariable UUID id, @Valid @RequestBody OrderStatus f, HttpServletRequest r) {
    return ok(operations.orderStatus(id, f), r);
  }

  @PatchMapping("/orders/{id}/items/{itemId}/fulfillment")
  public ResponseEntity<ApiResponse<OperationsManagementService.OrderView>> fulfill(
      @PathVariable UUID id,
      @PathVariable UUID itemId,
      @Valid @RequestBody Fulfillment f,
      HttpServletRequest r) {
    return ok(operations.fulfill(id, itemId, f), r);
  }

  @GetMapping("/payments")
  public ResponseEntity<ApiResponse<PageData<ManagementMapper.PaymentView>>> payments(
      @RequestParam(required = false) UUID orderId,
      @RequestParam(required = false) Payment.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.payments(orderId, status, page, size), r);
  }

  @GetMapping("/listings")
  public ResponseEntity<ApiResponse<PageData<Item>>> listings(
      @RequestParam(required = false) UUID poiId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.listings(poiId, page, size), r);
  }

  @DeleteMapping("/listings/{id}")
  public ResponseEntity<ApiResponse<Void>> listing(@PathVariable UUID id, HttpServletRequest r) {
    operations.deactivateListing(id);
    return ok(null, r);
  }

  @GetMapping("/reviews/{kind:poi|product}")
  public ResponseEntity<ApiResponse<PageData<ManagementMapper.Review>>> reviews(
      @PathVariable String kind,
      @RequestParam(required = false) ReviewStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.reviews(kind, status, page, size), r);
  }

  @PatchMapping("/reviews/{kind:poi|product}/{id}")
  public ResponseEntity<ApiResponse<Void>> moderate(
      @PathVariable String kind,
      @PathVariable UUID id,
      @Valid @RequestBody Moderation f,
      HttpServletRequest r) {
    operations.moderate(kind, id, f);
    return ok(null, r);
  }

  @GetMapping("/audits")
  public ResponseEntity<ApiResponse<PageData<OperationsManagementService.Audit>>> audits(
      @RequestParam(required = false) String targetType,
      @RequestParam(required = false) UUID targetId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(operations.audit(targetType, targetId, page, size), r);
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(T data, HttpServletRequest r) {
    var body =
        ApiResponse.success(HttpStatus.OK, "Xử lý yêu cầu thành công", data, r.getRequestURI());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
  }

  private <T> ResponseEntity<ApiResponse<T>> created(T data, HttpServletRequest r) {
    var body =
        ApiResponse.success(HttpStatus.CREATED, "Tạo dữ liệu thành công", data, r.getRequestURI());
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(body);
  }
}
