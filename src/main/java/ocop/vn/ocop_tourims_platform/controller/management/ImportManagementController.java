package ocop.vn.ocop_tourims_platform.controller.management;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.Version;
import ocop.vn.ocop_tourims_platform.service.management.ImportManagementService;
import ocop.vn.ocop_tourims_platform.service.management.ImportManagementService.Preview;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/management/imports")
public class ImportManagementController {
  private final ImportManagementService service;

  @PostMapping(value = "/{resource}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Preview>> preview(
      @PathVariable String resource,
      @RequestPart MultipartFile file,
      @RequestParam(required = false) String sheetName,
      @RequestParam(defaultValue = "1") int headerRow,
      @RequestPart Map<String, String> mapping,
      @RequestPart(required = false) Map<String, Object> defaults,
      HttpServletRequest r) {
    return ok(
        service.preview(
            resource, file, sheetName, headerRow, mapping, defaults == null ? Map.of() : defaults),
        r);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Preview>> get(@PathVariable UUID id, HttpServletRequest r) {
    return ok(service.get(id), r);
  }

  @PutMapping("/{id}/rows")
  public ResponseEntity<ApiResponse<Preview>> edit(
      @PathVariable UUID id,
      @Valid @RequestBody ImportManagementService.Edit f,
      HttpServletRequest r) {
    return ok(service.edit(id, f), r);
  }

  @PostMapping("/{id}/commit")
  public ResponseEntity<ApiResponse<Preview>> commit(
      @PathVariable UUID id, @Valid @RequestBody Version f, HttpServletRequest r) {
    return ok(service.commit(id, f.version()), r);
  }

  private ResponseEntity<ApiResponse<Preview>> ok(Preview data, HttpServletRequest r) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            ApiResponse.success(
                HttpStatus.OK, "Xử lý dữ liệu nhập thành công", data, r.getRequestURI()));
  }
}
