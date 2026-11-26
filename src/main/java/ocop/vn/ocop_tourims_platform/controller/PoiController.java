package ocop.vn.ocop_tourims_platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.request.PoiFilter;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.service.HomeService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pois")
public class PoiController {
  private final HomeService service;

  @GetMapping
  public ResponseEntity<ApiResponse<PageData<PoiCard>>> search(
      @Valid @ModelAttribute PoiFilter filter,
      @AuthenticationPrincipal Jwt jwt,
      HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Tìm kiếm địa điểm thành công",
            service.search(filter, userId(jwt)),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @GetMapping("/map")
  public ResponseEntity<ApiResponse<PageData<Marker>>> map(
      @Valid @ModelAttribute PoiFilter filter, HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Lấy ghim bản đồ thành công",
            service.map(filter),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @GetMapping("/{poiId}")
  public ResponseEntity<ApiResponse<Detail>> detail(
      @PathVariable UUID poiId, @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Lấy địa điểm thành công",
            service.detail(poiId, userId(jwt)),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @GetMapping("/{poiId}/products")
  public ResponseEntity<ApiResponse<PageData<ProductCard>>> products(
      @PathVariable UUID poiId,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0 trở lên")
          int page,
      @RequestParam(defaultValue = "20")
          @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
          @Max(value = 100, message = "Kích thước trang tối đa là 100")
          int size,
      HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Lấy sản phẩm thành công",
            service.products(poiId, page, size),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @GetMapping("/{poiId}/reviews")
  public ResponseEntity<ApiResponse<PageData<Review>>> reviews(
      @PathVariable UUID poiId,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0 trở lên")
          int page,
      @RequestParam(defaultValue = "20")
          @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
          @Max(value = 100, message = "Kích thước trang tối đa là 100")
          int size,
      HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Lấy đánh giá thành công",
            service.reviews(poiId, page, size),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  private UUID userId(Jwt jwt) {
    return jwt == null ? null : UUID.fromString(jwt.getSubject());
  }
}
