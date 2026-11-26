package ocop.vn.ocop_tourims_platform.mapper;

import java.util.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HomeMapper {
  default Option area(Area a) {
    return new Option(a.getId(), a.getName(), null, null, null, List.of());
  }

  default Option category(PoiCategory c) {
    return new Option(
        c.getId(),
        c.getName(),
        c.getIconUrl(),
        c.getColorHex(),
        c.getParent() == null ? null : c.getParent().getId(),
        List.of());
  }

  default Option activity(Activity a) {
    return new Option(a.getId(), a.getName(), a.getIconUrl(), null, null, List.of());
  }

  default Option amenity(Amenity a) {
    return new Option(a.getId(), a.getName(), a.getIconUrl(), null, null, List.of());
  }

  default List<Option> categories(Poi p) {
    return p.getCategoryLinks().stream()
        .map(l -> category(l.getCategory()))
        .sorted(Comparator.comparing(Option::name))
        .toList();
  }

  default PoiCard card(Poi p, Double distance, boolean favorite) {
    String image =
        p.getMedia().stream()
            .filter(m -> m.getMediaType() == PoiMedia.Type.IMAGE)
            .findFirst()
            .map(PoiMedia::getMediaUrl)
            .orElse(null);
    return new PoiCard(
        p.getId(),
        p.getPoiId(),
        p.getName(),
        p.getAddress(),
        area(p.getArea()),
        image,
        categories(p),
        p.getRatingAverage(),
        p.getRatingCount(),
        distance,
        favorite);
  }

  default Detail detail(Poi p) {
    var g = p.getGeometry();
    return new Detail(
        card(p, null, false),
        g == null ? null : g.getLatitude(),
        g == null ? null : g.getLongitude(),
        p.getOpeningHours(),
        p.getMedia().stream()
            .map(
                m ->
                    new Media(
                        m.getId(), m.getMediaType().name(), m.getMediaUrl(), m.getSortOrder()))
            .toList(),
        p.getActivities().stream()
            .map(l -> l.getActivity())
            .filter(Activity::isActive)
            .map(this::activity)
            .toList(),
        p.getAmenities().stream()
            .map(l -> l.getAmenity())
            .filter(Amenity::isActive)
            .map(this::amenity)
            .toList());
  }

  default ProductCard product(ProductListing listing) {
    Product p = listing.getProduct();
    return new ProductCard(
        p.getId(),
        listing.getId(),
        p.getName(),
        p.getImages().stream().findFirst().map(ProductImage::getImageUrl).orElse(null),
        listing.getPriceVnd(),
        listing.getStockQuantity(),
        p.getCertifications().stream()
            .filter(
                c ->
                    c.getPublicationStatus() == PublicationStatus.PUBLISHED
                        && !c.getIssuedOn().isAfter(java.time.LocalDate.now())
                        && (c.getExpiresOn() == null
                            || !c.getExpiresOn().isBefore(java.time.LocalDate.now())))
            .map(
                c ->
                    new Certificate(
                        c.getName(), c.getType().name(), c.getOcopStars(), c.getExpiresOn()))
            .toList());
  }

  default Review review(PoiReview r) {
    return new Review(
        r.getId(),
        r.getAuthor().getDisplayName(),
        r.getAuthor().getAvatarUrl(),
        r.getRating(),
        r.getComment(),
        r.getCreatedAt(),
        r.getMedia().stream()
            .map(
                m ->
                    new Media(
                        m.getId(), m.getMediaType().name(), m.getMediaUrl(), m.getSortOrder()))
            .toList());
  }

  default Notice notice(Notification n) {
    return new Notice(
        n.getId(),
        n.getNotificationId(),
        n.getTitle(),
        n.getContent(),
        n.getTargetPath(),
        n.isRead(),
        n.getCreatedAt());
  }
}
