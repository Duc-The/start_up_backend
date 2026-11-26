package ocop.vn.ocop_tourims_platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.dto.response.UserResponse;
import ocop.vn.ocop_tourims_platform.service.HomeService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeController {
  private final HomeService service;

  @GetMapping
  public ResponseEntity<ApiResponse<UserResponse>> me(
      @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    return success(service.me(user(jwt)), "Lấy tài khoản thành công", request);
  }

  @GetMapping("/favorite-pois")
  public ResponseEntity<ApiResponse<PageData<PoiCard>>> favorites(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0 trở lên")
          int page,
      @RequestParam(defaultValue = "20")
          @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
          @Max(value = 100, message = "Kích thước trang tối đa là 100")
          int size,
      HttpServletRequest request) {
    return success(
        service.favorites(user(jwt), page, size), "Lấy địa điểm yêu thích thành công", request);
  }

  @PutMapping("/favorite-pois/{poiId}")
  public ResponseEntity<ApiResponse<Void>> favorite(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID poiId, HttpServletRequest request) {
    service.favorite(user(jwt), poiId);
    return success(null, "Lưu địa điểm yêu thích thành công", request);
  }

  @DeleteMapping("/favorite-pois/{poiId}")
  public ResponseEntity<ApiResponse<Void>> unfavorite(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID poiId, HttpServletRequest request) {
    service.unfavorite(user(jwt), poiId);
    return success(null, "Bỏ lưu địa điểm thành công", request);
  }

  @GetMapping("/notifications")
  public ResponseEntity<ApiResponse<PageData<Notice>>> notifications(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "false") boolean unread,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0 trở lên")
          int page,
      @RequestParam(defaultValue = "20")
          @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
          @Max(value = 100, message = "Kích thước trang tối đa là 100")
          int size,
      HttpServletRequest request) {
    return success(
        service.notifications(user(jwt), unread, page, size), "Lấy thông báo thành công", request);
  }

  @GetMapping("/notifications/unread-count")
  public ResponseEntity<ApiResponse<Long>> unread(
      @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    return success(service.unread(user(jwt)), "Lấy số thông báo chưa đọc thành công", request);
  }

  @PatchMapping("/notifications/{notificationId}/read")
  public ResponseEntity<ApiResponse<Void>> read(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID notificationId,
      HttpServletRequest request) {
    service.read(user(jwt), notificationId);
    return success(null, "Đánh dấu đã đọc thành công", request);
  }

  @PatchMapping("/notifications/read-all")
  public ResponseEntity<ApiResponse<Void>> readAll(
      @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
    service.readAll(user(jwt));
    return success(null, "Đánh dấu tất cả đã đọc thành công", request);
  }

  private UUID user(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  private <T> ResponseEntity<ApiResponse<T>> success(
      T data, String message, HttpServletRequest request) {
    var body = ApiResponse.success(HttpStatus.OK, message, data, request.getRequestURI());
    return ResponseEntity.status(body.status()).cacheControl(CacheControl.noStore()).body(body);
  }
}
