package ocop.vn.ocop_tourims_platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.service.HomeService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HomeController {
  private final HomeService service;

  @GetMapping("/home/filters")
  public ResponseEntity<ApiResponse<Filters>> filters(HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK, "Lấy bộ lọc thành công", service.filters(), request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }

  @GetMapping("/search/suggestions")
  public ResponseEntity<ApiResponse<List<Suggestion>>> suggestions(
      @RequestParam @Size(max = 200, message = "Từ khóa tối đa 200 ký tự") String keyword,
      @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "Giới hạn phải từ 1 đến 20")
          @Max(value = 20, message = "Giới hạn tối đa là 20")
          int limit,
      HttpServletRequest request) {
    var body =
        ApiResponse.success(
            HttpStatus.OK,
            "Lấy gợi ý tìm kiếm thành công",
            service.suggestions(keyword, limit),
            request.getRequestURI());
    return ResponseEntity.status(body.status()).body(body);
  }
}
