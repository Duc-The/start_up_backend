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
public class ContentManagementController {
  private final ContentManagementService content;
  private final ManagementService management;
  private final MediaManagementService media;

  @GetMapping("/{resource:pois|products|certifications|tours|stories|events}")
  public ResponseEntity<ApiResponse<PageData<Item>>> list(
      @PathVariable String resource,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) UUID businessId,
      @RequestParam(required = false) PublicationStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(management.list(resource, keyword, businessId, status, page, size), r);
  }

  @GetMapping("/{resource:pois|products|certifications|tours|stories|events}/{id}")
  public ResponseEntity<ApiResponse<Item>> detail(
      @PathVariable String resource, @PathVariable UUID id, HttpServletRequest r) {
    return ok(management.detail(resource, id), r);
  }

  @PostMapping(
      "/{resource:pois|products|certifications|tours|stories|events}/{id}/{action:submit|approve}")
  public ResponseEntity<ApiResponse<Item>> workflow(
      @PathVariable String resource,
      @PathVariable UUID id,
      @PathVariable String action,
      @Valid @RequestBody Version f,
      HttpServletRequest r) {
    return ok(management.workflow(resource, id, action, f.version(), null), r);
  }

  @PostMapping("/{resource:pois|products|certifications|tours|stories|events}/{id}/reject")
  public ResponseEntity<ApiResponse<Item>> reject(
      @PathVariable String resource,
      @PathVariable UUID id,
      @Valid @RequestBody ReviewDecision f,
      HttpServletRequest r) {
    return ok(management.workflow(resource, id, "reject", f.version(), f.reason()), r);
  }

  @DeleteMapping("/{resource:pois|products|certifications|tours|stories|events}/{id}")
  public ResponseEntity<ApiResponse<Item>> archive(
      @PathVariable String resource,
      @PathVariable UUID id,
      @RequestParam long version,
      HttpServletRequest r) {
    return ok(management.workflow(resource, id, "archive", version, null), r);
  }

  @GetMapping("/tours/{id}/stops")
  public ResponseEntity<ApiResponse<List<ManagementMapper.Stop>>> stops(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(content.stops(id), r);
  }

  @PutMapping("/tours/{id}/stops")
  public ResponseEntity<ApiResponse<Item>> stops(
      @PathVariable UUID id, @Valid @RequestBody StopsForm f, HttpServletRequest r) {
    return ok(content.replaceStops(id, f), r);
  }

  @GetMapping("/tours/{id}/routes")
  public ResponseEntity<ApiResponse<List<ManagementMapper.Route>>> routes(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(content.routes(id), r);
  }

  @PutMapping("/tours/{id}/routes")
  public ResponseEntity<ApiResponse<Item>> route(
      @PathVariable UUID id, @Valid @RequestBody RouteForm f, HttpServletRequest r) {
    return ok(content.route(id, f), r);
  }

  @GetMapping("/{resource:pois|products|certifications|tours|stories|events|businesses}/{id}/media")
  public ResponseEntity<ApiResponse<List<ManagementMapper.Media>>> media(
      @PathVariable String resource, @PathVariable UUID id, HttpServletRequest r) {
    return ok(media.attachments(resource, id), r);
  }

  @PutMapping("/{resource:pois|products|certifications|tours|stories|events|businesses}/{id}/media")
  public ResponseEntity<ApiResponse<Void>> media(
      @PathVariable String resource,
      @PathVariable UUID id,
      @Valid @RequestBody MediaAttachment f,
      HttpServletRequest r) {
    media.attach(resource, id, f);
    return ok(null, r);
  }

  @PostMapping("/pois")
  public ResponseEntity<ApiResponse<Item>> createPoiForm(
      @Valid @RequestBody PoiForm f, HttpServletRequest r) {
    return created(content.poi(null, f), r);
  }

  @PutMapping("/pois/{id}")
  public ResponseEntity<ApiResponse<Item>> updatePoiForm(
      @PathVariable UUID id, @Valid @RequestBody PoiForm f, HttpServletRequest r) {
    return ok(content.poi(id, f), r);
  }

  @PostMapping("/products")
  public ResponseEntity<ApiResponse<Item>> createProductForm(
      @Valid @RequestBody ProductForm f, HttpServletRequest r) {
    return created(content.product(null, f), r);
  }

  @PutMapping("/products/{id}")
  public ResponseEntity<ApiResponse<Item>> updateProductForm(
      @PathVariable UUID id, @Valid @RequestBody ProductForm f, HttpServletRequest r) {
    return ok(content.product(id, f), r);
  }

  @PostMapping("/listings")
  public ResponseEntity<ApiResponse<Item>> createListingForm(
      @Valid @RequestBody ListingForm f, HttpServletRequest r) {
    return created(content.listing(null, f), r);
  }

  @PutMapping("/listings/{id}")
  public ResponseEntity<ApiResponse<Item>> updateListingForm(
      @PathVariable UUID id, @Valid @RequestBody ListingForm f, HttpServletRequest r) {
    return ok(content.listing(id, f), r);
  }

  @PostMapping("/certifications")
  public ResponseEntity<ApiResponse<Item>> createCertificateForm(
      @Valid @RequestBody CertificateForm f, HttpServletRequest r) {
    return created(content.certificate(null, f), r);
  }

  @PutMapping("/certifications/{id}")
  public ResponseEntity<ApiResponse<Item>> updateCertificateForm(
      @PathVariable UUID id, @Valid @RequestBody CertificateForm f, HttpServletRequest r) {
    return ok(content.certificate(id, f), r);
  }

  @PostMapping("/stories")
  public ResponseEntity<ApiResponse<Item>> createStoryForm(
      @Valid @RequestBody StoryForm f, HttpServletRequest r) {
    return created(content.story(null, f), r);
  }

  @PutMapping("/stories/{id}")
  public ResponseEntity<ApiResponse<Item>> updateStoryForm(
      @PathVariable UUID id, @Valid @RequestBody StoryForm f, HttpServletRequest r) {
    return ok(content.story(id, f), r);
  }

  @PostMapping("/events")
  public ResponseEntity<ApiResponse<Item>> createEventForm(
      @Valid @RequestBody EventForm f, HttpServletRequest r) {
    return created(content.event(null, f), r);
  }

  @PutMapping("/events/{id}")
  public ResponseEntity<ApiResponse<Item>> updateEventForm(
      @PathVariable UUID id, @Valid @RequestBody EventForm f, HttpServletRequest r) {
    return ok(content.event(id, f), r);
  }

  @PostMapping("/tours")
  public ResponseEntity<ApiResponse<Item>> createTourForm(
      @Valid @RequestBody TourForm f, HttpServletRequest r) {
    return created(content.tour(null, f), r);
  }

  @PutMapping("/tours/{id}")
  public ResponseEntity<ApiResponse<Item>> updateTourForm(
      @PathVariable UUID id, @Valid @RequestBody TourForm f, HttpServletRequest r) {
    return ok(content.tour(id, f), r);
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
