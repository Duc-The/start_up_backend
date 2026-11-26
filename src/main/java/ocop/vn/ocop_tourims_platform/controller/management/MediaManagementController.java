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
@RequestMapping("/api/v1/management/media")
public class MediaManagementController {
  private final MediaManagementService media;
  private final CatalogImageService catalogImages;

  @PostMapping("/catalog-images/upload-signature")
  public ResponseEntity<ApiResponse<UploadTicket>> catalogUpload(HttpServletRequest r) {
    return created(catalogImages.upload(), r);
  }

  @PostMapping("/catalog-images/complete")
  public ResponseEntity<ApiResponse<CatalogImageService.Image>> catalogComplete(
      @Valid @RequestBody CatalogImageService.Complete f, HttpServletRequest r) {
    return ok(catalogImages.complete(f), r);
  }

  @PostMapping("/upload-signature")
  public ResponseEntity<ApiResponse<UploadTicket>> upload(
      @Valid @RequestBody Upload f, HttpServletRequest r) {
    return created(media.upload(f), r);
  }

  @PostMapping("/{id}/complete")
  public ResponseEntity<ApiResponse<ManagementMapper.Media>> complete(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(media.complete(id), r);
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageData<ManagementMapper.Media>>> list(
      @RequestParam(required = false) UUID businessId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String resourceType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ok(media.list(businessId, keyword, resourceType, page, size), r);
  }

  @GetMapping("/{id}/usage")
  public ResponseEntity<ApiResponse<List<MediaManagementService.Usage>>> usage(
      @PathVariable UUID id, HttpServletRequest r) {
    return ok(media.usage(id), r);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<ManagementMapper.Media>> alt(
      @PathVariable UUID id, @Valid @RequestBody AltText f, HttpServletRequest r) {
    return ok(media.alt(id, f), r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest r) {
    media.delete(id);
    return ok(null, r);
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
