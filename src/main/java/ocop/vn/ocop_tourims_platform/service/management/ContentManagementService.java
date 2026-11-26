package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.Item;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentManagementService {
  private final ManagementAccess access;
  private final ManagementService management;
  private final ManagementMapper mapper;
  private final ManagementChanges changes;
  private final PoiManagementRepository pois;
  private final AreaManagementRepository areas;
  private final PoiCategoryManagementRepository categories;
  private final ActivityManagementRepository activities;
  private final AmenityManagementRepository amenities;
  private final ProductManagementRepository products;
  private final ProductTypeManagementRepository types;
  private final ProductListingManagementRepository listings;
  private final CertificationManagementRepository certificates;
  private final StoryManagementRepository stories;
  private final TourismEventManagementRepository events;
  private final TourManagementRepository tours;
  private final TourStopManagementRepository stops;
  private final TourRouteSegmentManagementRepository routes;
  private final MediaAssetManagementRepository assets;
  private final MediaLinkManagementRepository links;

  private <T> T get(ManagementRepository<T> repo, UUID id) {
    if (id == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    return repo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private void ownership(ManagedContent content, Business business, boolean created) {
    if (!created) {
      access.content(content);
      if (content.getBusiness() == null) access.requireAdmin();
      else if (!Objects.equals(ManagementMapper.id(content.getBusiness()), business.getId()))
        ManagementAccess.invalid("Không được thay đổi cơ sở sở hữu của nội dung");
    }
    content.setBusiness(business);
  }

  private <T extends ManagedContent> Item save(
      String type, ManagementRepository<T> repo, T c, boolean created, Long version) {
    management.draft(c, created, version);
    repo.saveAndFlush(c);
    changes.record(
        type, c, ManagementMapper.id(c.getBusiness()), created ? "CREATE" : "UPDATE", "");
    return mapper.item(c);
  }

  public Item poi(UUID id, PoiForm f) {
    boolean created = id == null;
    Poi p = created ? new Poi() : get(pois, id);
    ownership(p, access.business(f.businessId()), created);
    p.setArea(get(areas, f.areaId()));
    p.setName(f.name().strip());
    p.setAddress(f.address().strip());
    if ((f.lat() == null) != (f.lng() == null))
      ManagementAccess.invalid("Cần cung cấp đồng thời vĩ độ và kinh độ");
    if (f.lat() != null) p.setCoordinates(f.lat(), f.lng());
    else p.removeCoordinates();
    validateHours(f.openingHours());
    p.setOpeningHours(f.openingHours());
    // Reconcile links in place to avoid deleting and re-inserting the same unique key in one flush.
    new ArrayList<>(p.getCategoryLinks())
        .stream()
            .filter(l -> !f.categoryIds().contains(l.getCategory().getId()))
            .forEach(p::removeCategory);
    Set<UUID> oldCategories =
        new HashSet<>(p.getCategoryLinks().stream().map(l -> l.getCategory().getId()).toList());
    f.categoryIds().stream()
        .filter(i -> !oldCategories.contains(i))
        .forEach(i -> p.addCategory(get(categories, i)));
    new ArrayList<>(p.getActivities())
        .stream()
            .filter(l -> !f.activityIds().contains(l.getActivity().getId()))
            .forEach(p::removeActivity);
    Set<UUID> oldActivities =
        new HashSet<>(p.getActivities().stream().map(l -> l.getActivity().getId()).toList());
    f.activityIds().stream()
        .filter(i -> !oldActivities.contains(i))
        .forEach(i -> p.addActivity(get(activities, i)));
    new ArrayList<>(p.getAmenities())
        .stream()
            .filter(l -> !f.amenityIds().contains(l.getAmenity().getId()))
            .forEach(p::removeAmenity);
    Set<UUID> oldAmenities =
        new HashSet<>(p.getAmenities().stream().map(l -> l.getAmenity().getId()).toList());
    f.amenityIds().stream()
        .filter(i -> !oldAmenities.contains(i))
        .forEach(i -> p.addAmenity(get(amenities, i)));
    return save("pois", pois, p, created, f.version());
  }

  private void validateHours(Map<Poi.Day, List<Poi.OpeningInterval>> hours) {
    hours.forEach(
        (day, intervals) -> {
          if (day == null || intervals == null || intervals.size() > 10)
            ManagementAccess.invalid("Giờ mở cửa không hợp lệ");
          var ranges = new ArrayList<List<java.time.LocalTime>>();
          for (var interval : intervals) {
            if (interval == null
                || interval.open() == null
                || interval.close() == null
                || !interval.open().matches("\\d{2}:\\d{2}")
                || !interval.close().matches("\\d{2}:\\d{2}"))
              ManagementAccess.invalid("Giờ mở cửa phải có định dạng HH:mm");
            try {
              var from = java.time.LocalTime.parse(interval.open());
              var to = java.time.LocalTime.parse(interval.close());
              if (!from.isBefore(to))
                ManagementAccess.invalid("Giờ đóng cửa phải sau giờ mở cửa trong cùng ngày");
              if (ranges.stream().anyMatch(r -> from.isBefore(r.get(1)) && to.isAfter(r.get(0))))
                ManagementAccess.invalid("Các khung giờ không được chồng nhau");
              ranges.add(List.of(from, to));
            } catch (java.time.format.DateTimeParseException e) {
              ManagementAccess.invalid("Giờ mở cửa không hợp lệ");
            }
          }
        });
  }

  public Item product(UUID id, ProductForm f) {
    boolean created = id == null;
    Product p = created ? new Product() : get(products, id);
    Poi manufacturer = get(pois, f.manufacturerPoiId());
    access.content(manufacturer);
    if (manufacturer.getBusiness() == null)
      ManagementAccess.invalid("Cần gán cơ sở cho địa điểm sản xuất trước");
    ownership(p, access.business(manufacturer.getBusiness().getId()), created);
    p.setManufacturerPoi(manufacturer);
    p.setProductType(get(types, f.productTypeId()));
    p.setName(f.name().strip());
    p.setDescription(f.description().strip());
    return save("products", products, p, created, f.version());
  }

  public Item listing(UUID id, ListingForm f) {
    var poi = get(pois, f.poiId());
    access.content(poi);
    Product product = get(products, f.productId());
    if (!product.isActive()
        && !Objects.equals(
            ManagementMapper.id(product.getBusiness()), ManagementMapper.id(poi.getBusiness())))
      ManagementAccess.invalid("Chỉ được bán sản phẩm công khai hoặc của cơ sở mình");
    ProductListing l = id == null ? new ProductListing() : get(listings, id);
    if (id != null) {
      access.content(l.getPoi());
      if (!l.getPoi().getId().equals(f.poiId()) || !l.getProduct().getId().equals(f.productId()))
        ManagementAccess.invalid("Không được thay đổi địa điểm hoặc sản phẩm của điểm bán đã tạo");
    }
    l.setPoi(poi);
    l.setProduct(product);
    l.setPriceVnd(f.priceVnd());
    l.setStockQuantity(f.stockQuantity());
    l.setActive(f.active());
    access.stamp(l, id == null);
    listings.saveAndFlush(l);
    changes.record(
        "listings",
        l,
        ManagementMapper.id(poi.getBusiness()),
        id == null ? "CREATE" : "UPDATE",
        "");
    return mapper.item(l);
  }

  public Item certificate(UUID id, CertificateForm f) {
    if ((f.productId() == null) == (f.poiId() == null))
      ManagementAccess.invalid("Chứng nhận phải thuộc đúng một sản phẩm hoặc địa điểm");
    if (f.expiresOn() != null && f.expiresOn().isBefore(f.issuedOn()))
      ManagementAccess.invalid("Ngày hết hạn không được trước ngày cấp");
    boolean created = id == null;
    ManagedContent target =
        f.productId() != null ? get(products, f.productId()) : get(pois, f.poiId());
    access.content(target);
    if (target.getBusiness() == null) ManagementAccess.invalid("Nội dung chưa được gán cơ sở");
    if (f.poiId() != null && f.type() != Certification.Type.OTHER)
      ManagementAccess.invalid("Chứng nhận OCOP phải gắn với sản phẩm");
    if (f.type() == Certification.Type.OCOP && f.ocopStars() == null
        || f.type() == Certification.Type.OTHER && f.ocopStars() != null)
      ManagementAccess.invalid("Số sao chỉ bắt buộc cho chứng nhận OCOP");
    Certification c =
        created
            ? (target instanceof Product p
                ? new Certification(p, f.type(), f.ocopStars())
                : new Certification((Poi) target))
            : get(certificates, id);
    if (!created
        && (!Objects.equals(ManagementMapper.id(c.getProduct()), f.productId())
            || !Objects.equals(ManagementMapper.id(c.getPoi()), f.poiId())
            || c.getType() != f.type()
            || !Objects.equals(c.getOcopStars(), f.ocopStars())))
      ManagementAccess.invalid("Không được đổi đối tượng, loại hoặc số sao của chứng nhận đã tạo");
    ownership(c, access.business(target.getBusiness().getId()), created);
    c.setName(f.name());
    c.setCertificateNumber(f.certificateNumber());
    c.setIssuingAuthority(f.issuingAuthority());
    c.setIssuedOn(f.issuedOn());
    c.setExpiresOn(f.expiresOn());
    return save("certifications", certificates, c, created, f.version());
  }

  public Item story(UUID id, StoryForm f) {
    boolean created = id == null;
    Story s = created ? new Story() : get(stories, id);
    Poi poi = get(pois, f.poiId());
    access.content(poi);
    if (poi.getBusiness() == null) ManagementAccess.invalid("Địa điểm chưa được gán cơ sở");
    ownership(s, access.business(poi.getBusiness().getId()), created);
    s.setPoi(poi);
    s.setName(f.name());
    s.setSlug(f.slug());
    s.setIntroduction(f.introduction());
    s.setFounder(f.founder());
    s.setFoundedYear(f.foundedYear());
    Set<UUID> ids = new LinkedHashSet<>();
    f.sections().forEach(section -> ids.addAll(section.mediaIds()));
    if (ids.size() > 50)
      ManagementAccess.invalid("Một câu chuyện chỉ được gắn tối đa 50 tài nguyên khác nhau");
    Map<UUID, MediaAsset> selected = new HashMap<>();
    if (!ids.isEmpty())
      assets
          .lockAll(ids)
          .forEach(
              asset -> {
                if (!asset.getBusiness().getId().equals(s.getBusiness().getId())
                    || !asset.getStatus().equals("READY"))
                  ManagementAccess.invalid(
                      "Ảnh/video câu chuyện phải sẵn sàng và thuộc cùng cơ sở");
                selected.put(asset.getId(), asset);
              });
    if (selected.size() != ids.size()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    s.setSections(
        f.sections().stream()
            .map(v -> new Story.Section(v.heading(), v.body(), v.mediaIds()))
            .toList());
    Item result = save("stories", stories, s, created, f.version());
    // References embedded in JSON must also be tracked to protect assets from deletion.
    var old =
        links.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("targetType"), "story-sections"),
                    b.equal(r.get("targetId"), s.getId())));
    links.deleteAll(old);
    links.flush();
    int order = 0;
    for (UUID mediaId : ids) {
      MediaLink l = new MediaLink();
      l.setTargetType("story-sections");
      l.setTargetId(s.getId());
      l.setAsset(selected.get(mediaId));
      l.setSortOrder(order++);
      access.stamp(l, true);
      links.save(l);
    }
    return result;
  }

  public Item event(UUID id, EventForm f) {
    if (!f.endsAt().isAfter(f.startsAt()))
      ManagementAccess.invalid("Thời gian kết thúc phải sau thời gian bắt đầu");
    safeUrl(f.registrationUrl());
    boolean created = id == null;
    TourismEvent e = created ? new TourismEvent() : get(events, id);
    Poi poi = get(pois, f.poiId());
    access.content(poi);
    if (poi.getBusiness() == null) ManagementAccess.invalid("Địa điểm chưa được gán cơ sở");
    ownership(e, access.business(poi.getBusiness().getId()), created);
    e.setPoi(poi);
    e.setName(f.name());
    e.setSlug(f.slug());
    e.setDescription(f.description());
    e.setStartsAt(f.startsAt());
    e.setEndsAt(f.endsAt());
    e.setOrganizer(f.organizer());
    e.setRegistrationUrl(f.registrationUrl());
    e.setRecurrence(f.recurrence());
    return save("events", events, e, created, f.version());
  }

  public Item tour(UUID id, TourForm f) {
    boolean created = id == null;
    Tour t = created ? new Tour() : get(tours, id);
    ownership(t, access.business(f.businessId()), created);
    t.setName(f.name());
    t.setSlug(f.slug());
    t.setDescription(f.description());
    return save("tours", tours, t, created, f.version());
  }

  @Transactional(readOnly = true)
  public List<ManagementMapper.Stop> stops(UUID tourId) {
    Tour t = get(tours, tourId);
    access.content(t);
    return t.getStops().stream().map(mapper::stop).toList();
  }

  public Item replaceStops(UUID id, StopsForm f) {
    Tour t = get(tours, id);
    access.content(t);
    management.draft(t, false, f.version());
    // Delete route segments before deleting stops; flush avoids unique stop-order collisions.
    var old = stops.findAll((r, q, b) -> b.equal(r.get("tour").get("id"), id));
    if (!old.isEmpty()) {
      var ids = old.stream().map(TourStop::getId).toList();
      routes.deleteAll(
          routes.findAll(
              (r, q, b) ->
                  b.or(r.get("fromStop").get("id").in(ids), r.get("toStop").get("id").in(ids))));
      routes.flush();
    }
    t.clearStops();
    tours.flush();
    for (var stop : f.stops()) {
      Poi poi = get(pois, stop.poiId());
      if (!poi.isActive()
          && !Objects.equals(
              ManagementMapper.id(poi.getBusiness()), ManagementMapper.id(t.getBusiness())))
        ManagementAccess.invalid("Điểm dừng chưa được công khai");
      t.addStop(poi, stop.visitDurationMinutes());
    }
    tours.saveAndFlush(t);
    changes.record("tours", t, ManagementMapper.id(t.getBusiness()), "STOPS_UPDATE", "");
    return mapper.item(t);
  }

  @Transactional(readOnly = true)
  public List<ManagementMapper.Route> routes(UUID tourId) {
    Tour t = get(tours, tourId);
    access.content(t);
    return routes
        .findAll((r, q, b) -> b.equal(r.get("fromStop").get("tour").get("id"), tourId))
        .stream()
        .map(mapper::route)
        .toList();
  }

  public Item route(UUID tourId, RouteForm f) {
    Tour t = get(tours, tourId);
    access.content(t);
    ManagementAccess.version(t, f.version());
    TourStop from = get(stops, f.fromStopId()), to = get(stops, f.toStopId());
    if (!from.getTour().getId().equals(tourId)
        || !to.getTour().getId().equals(tourId)
        || from.getStopOrder() + 1 != to.getStopOrder())
      ManagementAccess.invalid("Hai điểm dừng phải liên tiếp và thuộc tour đang sửa");
    var existing =
        routes.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("fromStop").get("id"), from.getId()),
                    b.equal(r.get("toStop").get("id"), to.getId())));
    var route =
        existing.isEmpty()
            ? new TourRouteSegment(
                from,
                to,
                f.travelMode(),
                f.pathCoordinates(),
                f.distanceMeters(),
                f.durationSeconds())
            : existing.getFirst();
    route.updateRoute(f.travelMode(), f.pathCoordinates(), f.distanceMeters(), f.durationSeconds());
    access.stamp(route, existing.isEmpty());
    routes.save(route);
    return save("tours", tours, t, false, f.version());
  }

  public static void safeUrl(String value) {
    if (value == null || value.isBlank()) return;
    try {
      var uri = java.net.URI.create(value);
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
        ManagementAccess.invalid("Đường dẫn phải sử dụng HTTP hoặc HTTPS");
    } catch (IllegalArgumentException e) {
      ManagementAccess.invalid("Đường dẫn không hợp lệ");
    }
  }
}
