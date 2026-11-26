package ocop.vn.ocop_tourims_platform.controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.request.DirectionsRequest;
import ocop.vn.ocop_tourims_platform.dto.response.DirectionsResponse;
import ocop.vn.ocop_tourims_platform.service.DirectionsService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pois/directions")
public class DirectionsController {
  private final DirectionsService service;
  @PostMapping
  public ResponseEntity<ApiResponse<DirectionsResponse>> route(@Valid @RequestBody DirectionsRequest form, HttpServletRequest request) {
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Tìm lộ trình thành công", service.route(form), request.getRequestURI()));
  }
}
