package ocop.vn.ocop_tourims_platform.service.management;

import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.Item;
import ocop.vn.ocop_tourims_platform.entity.ImportBatch;
import ocop.vn.ocop_tourims_platform.repository.management.ImportBatchManagementRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportManagementService {
  private final ManagementAccess access;
  private final ContentManagementService content;
  private final BusinessManagementService businesses;
  private final ManagementStore store;
  private final ImportBatchManagementRepository batches;
  private final ManagementChanges changes;
  private final Validator validator;
  private final ObjectMapper json;

  public record RowCheck(
      int row, Map<String, Object> data, List<String> errors, List<String> warnings) {}

  public record Preview(
      UUID id,
      String resource,
      String status,
      long version,
      Instant expiresAt,
      List<RowCheck> rows,
      List<UUID> importedIds) {}

  public record Edit(
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.Size(min = 1, max = 500)
          List<Map<String, Object>> rows,
      @jakarta.validation.constraints.NotNull Long version) {}

  private Class<?> form(String resource) {
    return switch (resource) {
      case "businesses" -> BusinessForm.class;
      case "pois" -> PoiForm.class;
      case "products" -> ProductForm.class;
      case "listings" -> ListingForm.class;
      case "certifications" -> CertificateForm.class;
      case "stories" -> StoryForm.class;
      case "events" -> EventForm.class;
      case "tours" -> TourForm.class;
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    };
  }

  public Preview preview(
      String resource,
      MultipartFile file,
      String sheetName,
      int headerRow,
      Map<String, String> mapping,
      Map<String, Object> defaults) {
    access.requireAdmin();
    form(resource);
    if (file.isEmpty()
        || file.getSize() > 5L * 1024 * 1024
        || headerRow < 1
        || headerRow > 20
        || mapping.isEmpty()
        || mapping.size() > 40)
      ManagementAccess.invalid(
          "File tối đa 5 MB, dòng tiêu đề từ 1 đến 20, cần khai báo ánh xạ cột");
    List<Map<String, Object>> rows = new ArrayList<>();
    try (var stream = file.getInputStream();
        var workbook = WorkbookFactory.create(stream)) {
      Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
      if (sheet == null || sheet.getLastRowNum() > headerRow + 500)
        ManagementAccess.invalid("Sheet không tồn tại hoặc vượt quá 500 dòng dữ liệu");
      DataFormatter formatter = new DataFormatter(Locale.ROOT);
      Row header = sheet.getRow(headerRow - 1);
      if (header == null || header.getLastCellNum() > 100)
        ManagementAccess.invalid("Dòng tiêu đề không hợp lệ hoặc có quá 100 cột");
      Map<String, Integer> columns = new HashMap<>();
      for (Cell cell : header) {
        String name = formatter.formatCellValue(cell).strip();
        if (!name.isEmpty() && columns.putIfAbsent(name, cell.getColumnIndex()) != null)
          ManagementAccess.invalid("Tên cột trong dòng tiêu đề bị trùng");
      }
      for (String column : mapping.values())
        if (!columns.containsKey(column))
          ManagementAccess.invalid("Không tìm thấy cột trong ánh xạ Excel");
      for (int index = headerRow; index <= sheet.getLastRowNum(); index++) {
        Row row = sheet.getRow(index);
        if (row == null) continue;
        Map<String, Object> values = new LinkedHashMap<>(defaults);
        boolean hasData = false;
        for (var entry : mapping.entrySet()) {
          Cell cell = row.getCell(columns.get(entry.getValue()));
          if (cell != null && cell.getCellType() == CellType.FORMULA)
            ManagementAccess.invalid(
                "Không hỗ trợ ô công thức, hãy chuyển thành giá trị trước khi nhập");
          String value = formatter.formatCellValue(cell).strip();
          if (value.length() > 100000) ManagementAccess.invalid("Nội dung ô vượt quá giới hạn");
          if (!value.isEmpty()) {
            hasData = true;
            values.put(entry.getKey(), cellValue(value));
          }
        }
        if (hasData) rows.add(values);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      ManagementAccess.invalid(
          "Không đọc được file Excel, vui lòng kiểm tra định dạng và nội dung");
    }
    if (rows.isEmpty()) ManagementAccess.invalid("File không có dữ liệu để nhập");
    ImportBatch batch = new ImportBatch();
    batch.setResource(resource);
    batch.setRows(rows);
    batch.setExpiresAt(Instant.now().plusSeconds(86400));
    access.stamp(batch, true);
    batches.saveAndFlush(batch);
    return view(batch);
  }

  private Object cellValue(String value) {
    if (value.startsWith("[") || value.startsWith("{")) {
      try {
        return json.readValue(value, Object.class);
      } catch (RuntimeException e) {
        ManagementAccess.invalid("Ô danh sách hoặc đối tượng phải chứa JSON hợp lệ");
      }
    }
    return value;
  }

  private ImportBatch owned(UUID id) {
    access.requireAdmin();
    var batch =
        batches.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    if (!access.actor().equals(batch.getCreatedBy())) ManagementAccess.denied();
    return batch;
  }

  public Preview get(UUID id) {
    return view(owned(id));
  }

  public Preview edit(UUID id, Edit f) {
    ImportBatch batch = owned(id);
    editable(batch, f.version());
    batch.setRows(f.rows());
    access.stamp(batch, false);
    batches.flush();
    return view(batch);
  }

  private void editable(ImportBatch batch, long version) {
    if (batch.getVersion() != version) throw new BusinessException(ErrorCode.DATA_CONFLICT);
    if (!batch.getStatus().equals("PREVIEW") || batch.getExpiresAt().isBefore(Instant.now()))
      ManagementAccess.invalid("Đợt nhập đã hoàn thành hoặc hết hạn");
  }

  private Preview view(ImportBatch batch) {
    List<RowCheck> checks = new ArrayList<>();
    Set<String> names = new HashSet<>();
    int row = 1;
    for (var values : batch.getRows()) {
      List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
      try {
        Object f = json.convertValue(values, form(batch.getResource()));
        validator.validate(f).stream()
            .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
            .forEach(v -> errors.add(v.getPropertyPath() + " — " + v.getMessage()));
      } catch (RuntimeException e) {
        errors.add("Dữ liệu chưa khớp cấu trúc yêu cầu của API");
      }
      String name =
          String.valueOf(values.getOrDefault("name", "")).strip().toLowerCase(Locale.ROOT);
      if (!name.isBlank() && !names.add(name))
        errors.add("Tên bị trùng trong đợt nhập, cần đối chiếu trước khi lưu");
      if (batch.getResource().equals("pois") && !values.containsKey("lat"))
        warnings.add("Chưa có tọa độ, địa điểm chưa xuất hiện trên bản đồ");
      if (!name.isBlank() && !Set.of("businesses", "listings").contains(batch.getResource())) {
        long matches =
            store
                .repository(batch.getResource())
                .count((r, q, b) -> b.equal(b.lower(r.get("name")), name));
        if (matches > 0 && batch.getStatus().equals("PREVIEW"))
          errors.add("Tên đã tồn tại trong hệ thống, cần đối chiếu hoặc cập nhật qua API sửa");
      }
      checks.add(new RowCheck(row++, values, List.copyOf(errors), List.copyOf(warnings)));
    }
    return new Preview(
        batch.getId(),
        batch.getResource(),
        batch.getStatus(),
        batch.getVersion(),
        batch.getExpiresAt(),
        checks,
        batch.getImportedIds());
  }

  public Preview commit(UUID id, long version) {
    ImportBatch batch = owned(id);
    if (batch.getStatus().equals("COMMITTED")) return view(batch);
    editable(batch, version);
    if (view(batch).rows().stream().anyMatch(r -> !r.errors().isEmpty()))
      ManagementAccess.invalid("Cần sửa hết lỗi trong bản xem trước trước khi nhập");
    List<UUID> ids = new ArrayList<>();
    for (var values : batch.getRows()) {
      Object f = json.convertValue(values, form(batch.getResource()));
      Item result =
          switch (f) {
            case BusinessForm v -> businesses.save(null, v);
            case PoiForm v -> content.poi(null, v);
            case ProductForm v -> content.product(null, v);
            case ListingForm v -> content.listing(null, v);
            case CertificateForm v -> content.certificate(null, v);
            case StoryForm v -> content.story(null, v);
            case EventForm v -> content.event(null, v);
            case TourForm v -> content.tour(null, v);
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED);
          };
      ids.add(result.id());
    }
    batch.setImportedIds(ids);
    batch.setStatus("COMMITTED");
    access.stamp(batch, false);
    batches.flush();
    changes.record("imports", batch, null, "COMMIT", Integer.toString(ids.size()));
    return view(batch);
  }
}
