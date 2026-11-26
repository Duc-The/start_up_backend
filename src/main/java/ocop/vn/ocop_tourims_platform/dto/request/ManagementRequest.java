package ocop.vn.ocop_tourims_platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.*;

public final class ManagementRequest {
  private ManagementRequest() {}

  public record BusinessForm(
      @NotBlank(message = "Tên cơ sở không được trống") @Size(max = 255) String name,
      @NotNull String introduction,
      @NotNull String representative,
      @NotNull String phone,
      @NotNull @Email(message = "Email không hợp lệ") String email,
      @NotNull String website,
      @NotNull @Size(max = 10) Map<String, String> socialLinks,
      boolean active,
      Long version) {}

  public record PoiForm(
      @NotNull UUID businessId,
      @NotNull UUID areaId,
      @NotBlank(message = "Tên địa điểm không được trống") String name,
      @NotBlank(message = "Địa chỉ không được trống") String address,
      @DecimalMin("-90") @DecimalMax("90") Double lat,
      @DecimalMin("-180") @DecimalMax("180") Double lng,
      @NotNull Map<Poi.Day, List<Poi.OpeningInterval>> openingHours,
      @NotNull @Size(max = 50) Set<UUID> categoryIds,
      @NotNull @Size(max = 50) Set<UUID> activityIds,
      @NotNull @Size(max = 50) Set<UUID> amenityIds,
      Long version) {}

  public record ProductForm(
      @NotNull UUID manufacturerPoiId,
      @NotNull UUID productTypeId,
      @NotBlank(message = "Tên sản phẩm không được trống") String name,
      @NotBlank(message = "Mô tả sản phẩm không được trống") String description,
      Long version) {}

  public record ListingForm(
      @NotNull UUID poiId,
      @NotNull UUID productId,
      @Positive(message = "Giá phải lớn hơn 0") long priceVnd,
      @Min(value = 0, message = "Tồn kho không được âm") int stockQuantity,
      boolean active) {}

  public record CertificateForm(
      UUID productId,
      UUID poiId,
      @NotNull Certification.Type type,
      @Min(3) @Max(5) Short ocopStars,
      @NotBlank String name,
      @NotBlank String certificateNumber,
      @NotBlank String issuingAuthority,
      @NotNull LocalDate issuedOn,
      LocalDate expiresOn,
      Long version) {}

  public record Section(
      @NotBlank String heading,
      @NotBlank String body,
      @NotNull @Size(max = 20) List<UUID> mediaIds) {}

  public record StoryForm(
      @NotNull UUID poiId,
      @NotBlank String name,
      @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug không hợp lệ")
          String slug,
      @NotBlank String introduction,
      @NotNull String founder,
      @Min(1) @Max(2100) Integer foundedYear,
      @NotNull @Size(max = 100) List<@Valid Section> sections,
      Long version) {}

  public record EventForm(
      @NotNull UUID poiId,
      @NotBlank String name,
      @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug không hợp lệ")
          String slug,
      @NotBlank String description,
      @NotNull Instant startsAt,
      @NotNull Instant endsAt,
      @NotBlank String organizer,
      @NotNull String registrationUrl,
      @NotNull String recurrence,
      Long version) {}

  public record TourForm(
      @NotNull UUID businessId,
      @NotBlank String name,
      @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug không hợp lệ")
          String slug,
      @NotBlank String description,
      Long version) {}

  public record StopForm(@NotNull UUID poiId, @Min(0) int visitDurationMinutes) {}

  public record StopsForm(
      @NotNull @Size(min = 1, max = 100) List<@Valid StopForm> stops, @NotNull Long version) {}

  public record RouteForm(
      @NotNull UUID fromStopId,
      @NotNull UUID toStopId,
      @NotNull TourRouteSegment.TravelMode travelMode,
      @NotNull @Size(min = 2, max = 10000) List<TourRouteSegment.Coordinate> pathCoordinates,
      @Min(0) int distanceMeters,
      @Min(0) int durationSeconds,
      @NotNull Long version) {}

  public record ReviewDecision(
      @NotBlank(message = "Lý do không được trống") String reason, @NotNull Long version) {}

  public record Version(@NotNull Long version) {}

  public record CatalogForm(
      @NotBlank String name,
      String code,
      String slug,
      String iconUrl,
      @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Mã màu không hợp lệ") String colorHex,
      UUID parentId,
      @Min(0) int sortOrder,
      boolean active) {
    public CatalogForm {
      code = ocop.vn.ocop_tourims_platform.common.CatalogCodes.normalize(code);
    }
  }

  public record Roles(
      @NotEmpty @Size(max = 3)
          Set<@Pattern(regexp = "CUSTOMER|PARTNER|ADMIN", message = "Vai trò không hợp lệ") String>
              roles) {}

  public record UserStatus(@NotNull User.Status status) {}

  public record OrderStatus(@NotNull Order.Status status) {}

  public record Moderation(@NotNull ReviewStatus status, @NotBlank String reason) {}

  public record Upload(
      @NotNull UUID businessId,
      @NotNull @Pattern(regexp = "image|video", message = "Chỉ hỗ trợ ảnh hoặc video")
          String resourceType,
      @NotNull @Size(max = 500) String altText) {}

  public record MediaAttachment(
      @NotNull @Size(max = 50) List<@NotNull UUID> mediaIds, @NotNull Long version) {}

  public record Fulfillment(@NotNull OrderItem.FulfillmentStatus status) {}

  public record AltText(@NotNull @Size(max = 500) String altText) {}
}
