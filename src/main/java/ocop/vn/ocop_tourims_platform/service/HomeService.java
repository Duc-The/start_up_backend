package ocop.vn.ocop_tourims_platform.service;

import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.PoiFilter;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.*;
import ocop.vn.ocop_tourims_platform.dto.response.UserResponse;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.*;
import ocop.vn.ocop_tourims_platform.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
  private final AreaRepository areas;
  private final ProductTypeRepository types;
  private final PoiCategoryRepository categories;
  private final ActivityRepository activities;
  private final AmenityRepository amenities;
  private final PoiRepository pois;
  private final FavoriteRepository favorites;
  private final NotificationRepository notifications;
  private final PoiReviewRepository reviews;
  private final ProductListingRepository listings;
  private final HomeQueryRepository query;
  private final HomeMapper mapper;
  private final UserMapper userMapper;
  private final UserRepository users;
  private final HomeCache cache;

  public Filters filters() {
    return cache.get(
        "home:v1:filters",
        Filters.class,
        Duration.ofMinutes(5),
        () -> {
          List<Option> productTypes =
              types.findAll(Sort.by("sortOrder", "name")).stream()
                  .filter(ProductType::isActive)
                  .map(
                      t ->
                          new Option(
                              t.getId(),
                              t.getName(),
                              t.getIconUrl(),
                              null,
                              t.getParent() == null ? null : t.getParent().getId(),
                              List.of()))
                  .toList();
          List<Option> poiCategories =
              categories.findAll(Sort.by("name")).stream().map(mapper::category).toList();
          return new Filters(
              areas.findAll(Sort.by("name")).stream()
                  .filter(Area::isActive)
                  .map(mapper::area)
                  .toList(),
              tree(productTypes, null),
              tree(poiCategories, null),
              activities.findAll(Sort.by("sortOrder", "name")).stream()
                  .filter(Activity::isActive)
                  .map(mapper::activity)
                  .toList(),
              amenities.findAll(Sort.by("sortOrder", "name")).stream()
                  .filter(Amenity::isActive)
                  .map(mapper::amenity)
                  .toList(),
              List.of(3, 4, 5));
        });
  }

  private List<Option> tree(List<Option> all, UUID parent) {
    return all.stream()
        .filter(o -> Objects.equals(o.parentId(), parent))
        .map(
            o ->
                new Option(
                    o.id(), o.name(), o.iconUrl(), o.colorHex(), o.parentId(), tree(all, o.id())))
        .toList();
  }

  public PageData<PoiCard> search(PoiFilter f, UUID user) {
    var result = query.search(f, false);
    Set<UUID> saved = saved(user, result.items());
    return new PageData<>(
        result.items().stream()
            .map(p -> mapper.card(p, distance(p, f.lat(), f.lng()), saved.contains(p.getId())))
            .toList(),
        result.total(),
        f.pageNumber(),
        f.pageSize());
  }

  public PageData<Marker> map(PoiFilter f) {
    var result = query.search(f, true);
    return new PageData<>(
        result.items().stream()
            .map(
                p ->
                    new Marker(
                        p.getId(),
                        p.getName(),
                        p.getGeometry().getLatitude(),
                        p.getGeometry().getLongitude(),
                        mapper.categories(p)))
            .toList(),
        result.total(),
        0,
        2000);
  }

  public Detail detail(UUID id, UUID user) {
    activePoi(id);
    Detail d =
        cache.get(
            "home:v1:poi:" + id,
            Detail.class,
            Duration.ofMinutes(2),
            () -> mapper.detail(activePoi(id)));
    boolean favorite = user != null && !favorites.favoriteIds(user, List.of(id)).isEmpty();
    PoiCard c = d.poi();
    PoiCard personal =
        new PoiCard(
            c.id(),
            c.poiId(),
            c.name(),
            c.address(),
            c.area(),
            c.imageUrl(),
            c.categories(),
            c.ratingAverage(),
            c.ratingCount(),
            null,
            favorite);
    return new Detail(
        personal, d.lat(), d.lng(), d.openingHours(), d.media(), d.activities(), d.amenities());
  }

  public PageData<ProductCard> products(UUID id, int page, int size) {
    activePoi(id);
    var data =
        listings.findByPoiIdAndActiveTrueAndProductActiveTrue(id, pageable(page, size, "id"));
    return new PageData<>(
        data.getContent().stream().map(mapper::product).toList(),
        data.getTotalElements(),
        page,
        size);
  }

  public PageData<Review> reviews(UUID id, int page, int size) {
    activePoi(id);
    var data =
        reviews.findByPoiIdAndStatus(id, ReviewStatus.PUBLISHED, pageable(page, size, "createdAt"));
    return new PageData<>(
        data.getContent().stream().map(mapper::review).toList(),
        data.getTotalElements(),
        page,
        size);
  }

  public List<Suggestion> suggestions(String keyword, int limit) {
    if (keyword == null || keyword.isBlank()) return List.of();
    String normalized = keyword.strip().toLowerCase(Locale.ROOT);
    String key =
        "home:v2:suggestions:"
            + limit
            + ":"
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return cache
        .get(
            key,
            Suggestions.class,
            Duration.ofSeconds(30),
            () -> new Suggestions(query.suggestions(normalized, limit)))
        .items();
  }

  public PageData<PoiCard> favorites(UUID user, int page, int size) {
    var data = favorites.findByUserIdAndPoiActiveTrue(user, pageable(page, size, "createdAt"));
    return new PageData<>(
        data.getContent().stream().map(f -> mapper.card(f.getPoi(), null, true)).toList(),
        data.getTotalElements(),
        page,
        size);
  }

  @Transactional
  public void favorite(UUID user, UUID poi) {
    activePoi(poi);
    favorites.add(user, poi);
  }

  @Transactional
  public void unfavorite(UUID user, UUID poi) {
    favorites.remove(user, poi);
  }

  public PageData<Notice> notifications(UUID user, boolean unread, int page, int size) {
    var data =
        unread
            ? notifications.findByUserIdAndReadFalse(user, pageable(page, size, "createdAt"))
            : notifications.findByUserId(user, pageable(page, size, "createdAt"));
    return new PageData<>(
        data.getContent().stream().map(mapper::notice).toList(),
        data.getTotalElements(),
        page,
        size);
  }

  public long unread(UUID user) {
    return notifications.countByUserIdAndReadFalse(user);
  }

  @Transactional
  public void read(UUID user, UUID id) {
    notifications
        .findByIdAndUserId(id, user)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))
        .markAsRead();
  }

  @Transactional
  public void readAll(UUID user) {
    notifications.readAll(user);
  }

  public UserResponse me(UUID id) {
    User user =
        users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return userMapper.toResponse(
        user, user.getUserRoles().stream().map(r -> r.getRole().getCode()).sorted().toList());
  }

  private Poi activePoi(UUID id) {
    return pois.findById(id)
        .filter(Poi::isActive)
        .filter(p -> p.getBusiness() == null || p.getBusiness().isActive())
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Pageable pageable(int page, int size, String sort) {
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort).and(Sort.by("id")));
  }

  private Set<UUID> saved(UUID user, List<Poi> items) {
    if (user == null || items.isEmpty()) return Set.of();
    return favorites.favoriteIds(user, items.stream().map(Poi::getId).toList());
  }

  private Double distance(Poi p, Double lat, Double lng) {
    if (lat == null || p.getGeometry() == null) return null;
    var g = p.getGeometry();
    double a =
        Math.pow(Math.sin(Math.toRadians(g.getLatitude() - lat) / 2), 2)
            + Math.cos(Math.toRadians(lat))
                * Math.cos(Math.toRadians(g.getLatitude()))
                * Math.pow(Math.sin(Math.toRadians(g.getLongitude() - lng) / 2), 2);
    return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
  }
}
