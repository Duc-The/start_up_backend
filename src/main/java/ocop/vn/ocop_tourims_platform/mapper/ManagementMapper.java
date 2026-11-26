package ocop.vn.ocop_tourims_platform.mapper;

import java.util.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.Item;
import ocop.vn.ocop_tourims_platform.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ManagementMapper {
  public record Media(
      UUID id,
      String code,
      UUID businessId,
      String publicId,
      String resourceType,
      String status,
      String secureUrl,
      String originalFilename,
      String format,
      long bytes,
      int width,
      int height,
      Double duration,
      String altText) {}

  public record Stop(UUID id, UUID poiId, String name, int stopOrder, int visitDurationMinutes) {}

  public record Route(
      UUID id,
      UUID fromStopId,
      UUID toStopId,
      TourRouteSegment.TravelMode travelMode,
      List<TourRouteSegment.Coordinate> pathCoordinates,
      int distanceMeters,
      int durationSeconds) {}

  public record Review(
      UUID id,
      UUID authorId,
      UUID targetId,
      short rating,
      String comment,
      ReviewStatus status,
      List<?> media) {}

  public record OrderLine(
      UUID id, UUID listingId, String productName, int quantity, long unitPriceVnd) {}

  public record OrderView(
      UUID id,
      String orderId,
      Order.Status status,
      String recipientName,
      String recipientPhone,
      String deliveryAddress,
      List<OrderLine> items,
      Long totalVnd) {}

  public record PaymentView(
      UUID id,
      String paymentId,
      UUID orderId,
      Payment.Status status,
      long amountVnd,
      Long payosOrderCode,
      String payosPaymentLinkId) {}

  public record UserView(
      UUID id,
      String userId,
      String email,
      String displayName,
      User.Status status,
      Set<String> roles) {}

  public record Catalog(
      UUID id,
      String code,
      String name,
      String slug,
      String iconUrl,
      String colorHex,
      UUID parentId,
      int sortOrder,
      boolean active) {}

  public Media media(MediaAsset a) {
    return new Media(
        a.getId(),
        a.getCode(),
        a.getBusiness().getId(),
        a.getPublicId(),
        a.getResourceType(),
        a.getStatus(),
        a.getSecureUrl(),
        a.getOriginalFilename(),
        a.getFormat(),
        a.getBytes(),
        a.getWidth(),
        a.getHeight(),
        a.getDuration(),
        a.getAltText());
  }

  public Item item(BaseEntity entity) {
    String code;
    String name;
    Object details;
    switch (entity) {
      case Business b -> {
        code = b.getCode();
        name = b.getName();
        details =
            new BusinessForm(
                b.getName(),
                b.getIntroduction(),
                b.getRepresentative(),
                b.getPhone(),
                b.getEmail(),
                b.getWebsite(),
                b.getSocialLinks(),
                b.isActive(),
                b.getVersion());
      }
      case Poi p -> {
        code = p.getPoiId();
        name = p.getName();
        details =
            new PoiForm(
                id(p.getBusiness()),
                id(p.getArea()),
                p.getName(),
                p.getAddress(),
                p.getGeometry() == null ? null : p.getGeometry().getLatitude(),
                p.getGeometry() == null ? null : p.getGeometry().getLongitude(),
                p.getOpeningHours(),
                new HashSet<>(p.getCategoryLinks().stream().map(l -> id(l.getCategory())).toList()),
                new HashSet<>(p.getActivities().stream().map(l -> id(l.getActivity())).toList()),
                new HashSet<>(p.getAmenities().stream().map(l -> id(l.getAmenity())).toList()),
                p.getVersion());
      }
      case Product p -> {
        code = p.getProductId();
        name = p.getName();
        details =
            new ProductForm(
                id(p.getManufacturerPoi()),
                id(p.getProductType()),
                p.getName(),
                p.getDescription(),
                p.getVersion());
      }
      case ProductListing l -> {
        code = l.getProductListingId();
        name = l.getProduct().getName();
        details =
            new ListingForm(
                id(l.getPoi()),
                id(l.getProduct()),
                l.getPriceVnd(),
                l.getStockQuantity(),
                l.isActive());
      }
      case Certification c -> {
        code = c.getCertificationId();
        name = c.getName();
        details =
            new CertificateForm(
                id(c.getProduct()),
                id(c.getPoi()),
                c.getType(),
                c.getOcopStars(),
                c.getName(),
                c.getCertificateNumber(),
                c.getIssuingAuthority(),
                c.getIssuedOn(),
                c.getExpiresOn(),
                c.getVersion());
      }
      case Story s -> {
        code = s.getCode();
        name = s.getName();
        details =
            new StoryForm(
                id(s.getPoi()),
                s.getName(),
                s.getSlug(),
                s.getIntroduction(),
                s.getFounder(),
                s.getFoundedYear(),
                s.getSections().stream()
                    .map(v -> new Section(v.heading(), v.body(), v.mediaIds()))
                    .toList(),
                s.getVersion());
      }
      case TourismEvent e -> {
        code = e.getCode();
        name = e.getName();
        details =
            new EventForm(
                id(e.getPoi()),
                e.getName(),
                e.getSlug(),
                e.getDescription(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getOrganizer(),
                e.getRegistrationUrl(),
                e.getRecurrence(),
                e.getVersion());
      }
      case Tour t -> {
        code = t.getTourId();
        name = t.getName();
        details =
            new TourForm(
                id(t.getBusiness()), t.getName(), t.getSlug(), t.getDescription(), t.getVersion());
      }
      default -> throw new IllegalArgumentException("Loại dữ liệu không được hỗ trợ");
    }
    ManagedContent managed = entity instanceof ManagedContent c ? c : null;
    UUID business =
        managed != null
            ? id(managed.getBusiness())
            : entity instanceof Business b
                ? b.getId()
                : entity instanceof ProductListing l ? id(l.getPoi().getBusiness()) : null;
    return new Item(
        entity.getId(),
        code,
        name,
        business,
        managed == null ? null : managed.getPublicationStatus(),
        managed == null ? null : managed.getRejectionReason(),
        managed == null
            ? entity instanceof Business b ? b.getVersion() : null
            : managed.getVersion(),
        details);
  }

  public Stop stop(TourStop s) {
    return new Stop(
        s.getId(),
        id(s.getPoi()),
        s.getPoi().getName(),
        s.getStopOrder(),
        s.getVisitDurationMinutes());
  }

  public Route route(TourRouteSegment s) {
    return new Route(
        s.getId(),
        id(s.getFromStop()),
        id(s.getToStop()),
        s.getTravelMode(),
        s.getPathCoordinates(),
        s.getDistanceMeters(),
        s.getDurationSeconds());
  }

  public UserView user(User u) {
    return new UserView(
        u.getId(),
        u.getUserId(),
        u.getEmail(),
        u.getDisplayName(),
        u.getStatus(),
        new HashSet<>(u.getUserRoles().stream().map(r -> r.getRole().getCode()).toList()));
  }

  public PaymentView payment(Payment p) {
    return new PaymentView(
        p.getId(),
        p.getPaymentId(),
        id(p.getOrder()),
        p.getStatus(),
        p.getAmountVnd(),
        p.getPayosOrderCode(),
        p.getPayosPaymentLinkId());
  }

  public static UUID id(BaseEntity e) {
    return e == null ? null : e.getId();
  }
}
