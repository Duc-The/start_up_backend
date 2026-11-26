package ocop.vn.ocop_tourims_platform.dto.request;

import jakarta.validation.constraints.*;
import java.util.*;

public record PoiFilter(
    @Size(max = 200, message = "Từ khóa không được vượt quá 200 ký tự") String keyword,
    @Size(max = 50, message = "Chỉ được chọn tối đa 50 khu vực") List<UUID> areaIds,
    @Size(max = 50, message = "Chỉ được chọn tối đa 50 loại sản phẩm") List<UUID> productTypeIds,
    @Size(max = 50, message = "Chỉ được chọn tối đa 50 loại điểm") List<UUID> categoryIds,
    @Size(max = 50, message = "Chỉ được chọn tối đa 50 hoạt động") List<UUID> activityIds,
    @Size(max = 50, message = "Chỉ được chọn tối đa 50 tiện ích") List<UUID> amenityIds,
    List<
            @Min(value = 3, message = "Hạng OCOP phải từ 3 đến 5")
            @Max(value = 5, message = "Hạng OCOP phải từ 3 đến 5") Integer>
        ocopStars,
    @DecimalMin(value = "-90", message = "Vĩ độ không hợp lệ")
        @DecimalMax(value = "90", message = "Vĩ độ không hợp lệ")
        Double lat,
    @DecimalMin(value = "-180", message = "Kinh độ không hợp lệ")
        @DecimalMax(value = "180", message = "Kinh độ không hợp lệ")
        Double lng,
    @Min(value = 1, message = "Bán kính phải lớn hơn 0")
        @Max(value = 200000, message = "Bán kính tối đa là 200 km")
        Integer radiusMeters,
    Double minLat,
    Double maxLat,
    Double minLng,
    Double maxLng,
    String sort,
    @Min(value = 0, message = "Trang phải từ 0 trở lên") Integer page,
    @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100")
        @Max(value = 100, message = "Kích thước trang tối đa là 100")
        Integer size) {
  public int pageNumber() {
    return page == null ? 0 : page;
  }

  public int pageSize() {
    return size == null ? 20 : size;
  }

  @AssertTrue(message = "Cần truyền đủ tọa độ hợp lệ, bán kính và vùng bản đồ hợp lệ")
  public boolean isLocationValid() {
    if ((lat == null) != (lng == null) || (radiusMeters != null && lat == null)) return false;
    if (lat != null && (!Double.isFinite(lat) || !Double.isFinite(lng))) return false;
    int count =
        (minLat == null ? 0 : 1)
            + (maxLat == null ? 0 : 1)
            + (minLng == null ? 0 : 1)
            + (maxLng == null ? 0 : 1);
    return count == 0
        || (count == 4
            && Double.isFinite(minLat)
            && Double.isFinite(maxLat)
            && Double.isFinite(minLng)
            && Double.isFinite(maxLng)
            && minLat >= -90
            && maxLat <= 90
            && minLat <= maxLat
            && minLng >= -180
            && maxLng <= 180
            && minLng <= maxLng);
  }

  @AssertTrue(
      message = "Sắp xếp phải là relevance, distance hoặc rating; sắp xếp khoảng cách cần vị trí")
  public boolean isSortValid() {
    return sort == null
        || sort.equals("relevance")
        || sort.equals("rating")
        || (sort.equals("distance") && lat != null && lng != null);
  }
}
