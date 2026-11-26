package ocop.vn.ocop_tourims_platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record DirectionsRequest(
    @NotNull(message = "Cần chọn điểm đi và điểm đến")
    @Size(min = 2, max = 7, message = "Lộ trình gồm 2 đến 7 địa điểm")
    List<@NotNull(message = "Tọa độ không được để trống") @Valid Coordinate> points,
    @NotNull(message = "Cần chọn phương tiện")
    @Pattern(regexp = "car|motorcycle|foot", message = "Phương tiện phải là ô tô, xe máy hoặc đi bộ") String mode) {
  public record Coordinate(
      @NotNull(message = "Thiếu vĩ độ") @DecimalMin(value = "-90", message = "Vĩ độ không hợp lệ") @DecimalMax(value = "90", message = "Vĩ độ không hợp lệ") Double lat,
      @NotNull(message = "Thiếu kinh độ") @DecimalMin(value = "-180", message = "Kinh độ không hợp lệ") @DecimalMax(value = "180", message = "Kinh độ không hợp lệ") Double lng) {
    @AssertTrue(message = "Tọa độ không hợp lệ")
    public boolean isFinite() { return lat == null || lng == null || (Double.isFinite(lat) && Double.isFinite(lng)); }
    public String encoded() { return lat + "," + lng; }
  }
}
