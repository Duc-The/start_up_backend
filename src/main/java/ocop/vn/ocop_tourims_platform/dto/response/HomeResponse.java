package ocop.vn.ocop_tourims_platform.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class HomeResponse {
  private HomeResponse() {}

  public record PageData<T>(List<T> items, long total, int page, int size) {}

  public record Option(
      UUID id,
      String name,
      String iconUrl,
      String colorHex,
      UUID parentId,
      List<Option> children) {}

  public record Filters(
      List<Option> areas,
      List<Option> productTypes,
      List<Option> categories,
      List<Option> activities,
      List<Option> amenities,
      List<Integer> ocopStars) {}

  public record Media(UUID id, String type, String url, int sortOrder) {}

  public record PoiCard(
      UUID id,
      String poiId,
      String name,
      String address,
      Option area,
      String imageUrl,
      List<Option> categories,
      BigDecimal ratingAverage,
      int ratingCount,
      Double distanceMeters,
      boolean favorite) {}

  public record Marker(UUID id, String name, double lat, double lng, List<Option> categories) {}

  public record Detail(
      PoiCard poi,
      Double lat,
      Double lng,
      Map<?, ?> openingHours,
      List<Media> media,
      List<Option> activities,
      List<Option> amenities) {}

  public record Certificate(
      String name, String type, Short ocopStars, java.time.LocalDate expiresOn) {}

  public record ProductCard(
      UUID id,
      UUID listingId,
      String name,
      String imageUrl,
      long priceVnd,
      int stockQuantity,
      List<Certificate> certifications) {}

  public record Review(
      UUID id,
      String displayName,
      String avatarUrl,
      short rating,
      String comment,
      Instant createdAt,
      List<Media> media) {}

  public record Suggestion(UUID id, String type, String name) {}

  public record Suggestions(List<Suggestion> items) {}

  public record Notice(
      UUID id,
      String notificationId,
      String title,
      String content,
      String targetPath,
      boolean read,
      Instant createdAt) {}
}
