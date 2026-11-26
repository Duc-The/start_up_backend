package ocop.vn.ocop_tourims_platform.repository.specification;

import jakarta.persistence.criteria.*;
import ocop.vn.ocop_tourims_platform.common.SearchText;
import jakarta.persistence.criteria.Order;
import java.util.*;
import ocop.vn.ocop_tourims_platform.dto.request.PoiFilter;
import ocop.vn.ocop_tourims_platform.entity.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PoiSpecification {
  private PoiSpecification() {}

  public static Specification<Poi> filter(
      PoiFilter filter, Collection<UUID> types, Collection<UUID> categories, boolean map) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.isTrue(root.get("active")));
      Join<Poi, Business> business = root.join("business", JoinType.LEFT);
      predicates.add(cb.or(cb.isNull(business.get("id")), cb.isTrue(business.get("active"))));
      if (present(filter.areaIds()))
        predicates.add(root.get("area").get("id").in(filter.areaIds()));
      if (present(filter.categoryIds())) predicates.add(category(root, query, cb, categories));
      if (present(filter.activityIds()))
        predicates.add(activity(root, query, cb, filter.activityIds()));
      if (present(filter.amenityIds())) {
        for (UUID id : new HashSet<>(filter.amenityIds()))
          predicates.add(amenity(root, query, cb, id));
      }
      if (StringUtils.hasText(filter.keyword())) {
        List<Predicate> terms = new ArrayList<>();
        for (String term : searchTerms(filter.keyword())) {
          terms.add(cb.or(
              cb.like(normalized(cb, root.get("name")), pattern(term), '!'),
              cb.like(normalized(cb, root.get("address")), pattern(term), '!'),
              categoryKeyword(root, query, cb, term),
              products(root, query, cb, List.of(), List.of(), term)));
        }
        predicates.add(cb.or(terms.toArray(Predicate[]::new)));
      }
      if (present(filter.productTypeIds()) || present(filter.ocopStars())) {
        predicates.add(products(root, query, cb, types, filter.ocopStars(), null));
      }
      Join<Poi, PoiGeometry> geometry = root.join("geometry", JoinType.LEFT);
      if (map) predicates.add(cb.isNotNull(geometry.get("id")));
      if (filter.radiusMeters() != null) {
        double latitudeDelta = filter.radiusMeters() / 111000.0;
        predicates.add(cb.between(geometry.get("latitude"), Math.max(-90, filter.lat()-latitudeDelta), Math.min(90, filter.lat()+latitudeDelta)));
        double cosine = Math.cos(Math.toRadians(Math.min(90, Math.abs(filter.lat()) + latitudeDelta)));
        if (cosine > 0.01) {
          double longitudeDelta = latitudeDelta / cosine;
          if (filter.lng()-longitudeDelta >= -180 && filter.lng()+longitudeDelta <= 180)
            predicates.add(cb.between(geometry.get("longitude"), filter.lng()-longitudeDelta, filter.lng()+longitudeDelta));
        }
        predicates.add(
            cb.le(distance(cb, geometry, filter.lat(), filter.lng()), filter.radiusMeters()));
      }
      if (filter.minLat() != null) {
        predicates.add(cb.between(geometry.get("latitude"), filter.minLat(), filter.maxLat()));
        predicates.add(cb.between(geometry.get("longitude"), filter.minLng(), filter.maxLng()));
      }
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        List<Order> order = new ArrayList<>();
        if ("distance".equals(filter.sort())) {
          order.add(cb.asc(cb.selectCase().when(cb.isNull(geometry.get("id")), 1).otherwise(0)));
          order.add(cb.asc(distance(cb, geometry, filter.lat(), filter.lng())));
        } else if ("rating".equals(filter.sort())) {
          order.add(cb.desc(root.get("ratingAverage")));
          order.add(cb.desc(root.get("ratingCount")));
        } else if (StringUtils.hasText(filter.keyword())) {
          order.add(
              cb.asc(
                  cb.selectCase()
                      .when(
                          cb.equal(
                              cb.lower(root.get("name")),
                              filter.keyword().strip().toLowerCase(Locale.ROOT)),
                          0)
                      .when(cb.like(cb.lower(root.get("name")), pattern(filter.keyword()), '!'), 1)
                      .otherwise(2)));
          order.add(cb.asc(root.get("name")));
        } else {
          order.add(cb.asc(root.get("name")));
        }
        order.add(cb.asc(root.get("id")));
        query.orderBy(order);
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static Predicate categoryKeyword(Root<Poi> poi, CriteriaQuery<?> query, CriteriaBuilder cb, String keyword) {
    Subquery<Integer> sub = query.subquery(Integer.class);
    Root<PoiCategoryLink> link = sub.from(PoiCategoryLink.class);
    sub.select(cb.literal(1)).where(cb.equal(link.get("poi"), poi),
        cb.like(normalized(cb, link.get("category").get("name")), pattern(keyword), '!'));
    return cb.exists(sub);
  }

  private static Predicate category(
      Root<Poi> poi, CriteriaQuery<?> query, CriteriaBuilder cb, Collection<UUID> ids) {
    Subquery<Integer> sub = query.subquery(Integer.class);
    Root<PoiCategoryLink> link = sub.from(PoiCategoryLink.class);
    sub.select(cb.literal(1))
        .where(cb.equal(link.get("poi"), poi), link.get("category").get("id").in(ids));
    return cb.exists(sub);
  }

  private static Predicate activity(
      Root<Poi> poi, CriteriaQuery<?> query, CriteriaBuilder cb, Collection<UUID> ids) {
    Subquery<Integer> sub = query.subquery(Integer.class);
    Root<PoiActivity> link = sub.from(PoiActivity.class);
    sub.select(cb.literal(1))
        .where(
            cb.equal(link.get("poi"), poi),
            cb.isTrue(link.get("activity").get("active")),
            link.get("activity").get("id").in(ids));
    return cb.exists(sub);
  }

  private static Predicate amenity(
      Root<Poi> poi, CriteriaQuery<?> query, CriteriaBuilder cb, UUID id) {
    Subquery<Integer> sub = query.subquery(Integer.class);
    Root<PoiAmenity> link = sub.from(PoiAmenity.class);
    sub.select(cb.literal(1))
        .where(
            cb.equal(link.get("poi"), poi),
            cb.isTrue(link.get("amenity").get("active")),
            cb.equal(link.get("amenity").get("id"), id));
    return cb.exists(sub);
  }

  private static Predicate products(
      Root<Poi> poi,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      Collection<UUID> types,
      Collection<Integer> stars,
      String keyword) {
    Subquery<Integer> sub = query.subquery(Integer.class);
    Root<Product> product = sub.from(Product.class);
    Subquery<Integer> sale = sub.subquery(Integer.class);
    Root<ProductListing> listing = sale.from(ProductListing.class);
    sale.select(cb.literal(1))
        .where(
            cb.equal(listing.get("poi"), poi),
            cb.equal(listing.get("product"), product),
            cb.isTrue(listing.get("active")));
    List<Predicate> conditions = new ArrayList<>();
    conditions.add(cb.isTrue(product.get("active")));
    Join<Product, Business> manufacturerBusiness = product.join("business", JoinType.LEFT);
    conditions.add(
        cb.or(
            cb.isNull(manufacturerBusiness.get("id")),
            cb.isTrue(manufacturerBusiness.get("active"))));
    conditions.add(cb.or(cb.equal(product.get("manufacturerPoi"), poi), cb.exists(sale)));
    if (StringUtils.hasText(keyword))
      conditions.add(cb.like(normalized(cb, product.get("name")), pattern(keyword), '!'));
    if (present(types)) conditions.add(product.get("productType").get("id").in(types));
    // An explicitly selected but unknown type must match no products.
    if (types != null && types.isEmpty() && keyword == null) conditions.add(cb.disjunction());
    if (present(stars)) {
      Subquery<Integer> certificates = sub.subquery(Integer.class);
      Root<Certification> cert = certificates.from(Certification.class);
      certificates
          .select(cb.literal(1))
          .where(
              cb.equal(cert.get("product"), product),
              cb.equal(cert.get("type"), Certification.Type.OCOP),
              cb.equal(cert.get("publicationStatus"), PublicationStatus.PUBLISHED),
              cb.lessThanOrEqualTo(
                  cert.get("issuedOn"), cb.currentDate().as(java.time.LocalDate.class)),
              cb.or(
                  cb.isNull(cert.get("expiresOn")),
                  cb.greaterThanOrEqualTo(
                      cert.get("expiresOn"), cb.currentDate().as(java.time.LocalDate.class))),
              cert.get("ocopStars").in(stars));
      conditions.add(cb.exists(certificates));
    }
    sub.select(cb.literal(1)).where(conditions.toArray(Predicate[]::new));
    return cb.exists(sub);
  }

  private static Expression<Double> distance(
      CriteriaBuilder cb, Join<Poi, PoiGeometry> geometry, double lat, double lng) {
    Expression<Double> latitude = cb.function("radians", Double.class, geometry.get("latitude"));
    Expression<Double> originLat = cb.literal(Math.toRadians(lat));
    Expression<Double> longitudeDelta =
        cb.function("radians", Double.class, cb.diff(geometry.get("longitude"), lng));
    Expression<Double> cosine =
        cb.sum(
            cb.prod(
                cb.function("sin", Double.class, latitude),
                cb.function("sin", Double.class, originLat)),
            cb.prod(
                cb.prod(
                    cb.function("cos", Double.class, latitude),
                    cb.function("cos", Double.class, originLat)),
                cb.function("cos", Double.class, longitudeDelta)));
    Expression<Double> bounded =
        cb.function(
            "least",
            Double.class,
            cb.literal(1.0),
            cb.function("greatest", Double.class, cb.literal(-1.0), cosine));
    return cb.prod(6371000.0, cb.function("acos", Double.class, bounded));
  }

  private static Expression<String> normalized(CriteriaBuilder cb, Expression<String> value) {
    return cb.function("translate", String.class, cb.lower(value), cb.literal(SearchText.ACCENTS), cb.literal(SearchText.PLAIN));
  }

  private static List<String> searchTerms(String keyword) {
    String normalized = SearchText.normalize(keyword);
    return switch (normalized) {
      case "coffee", "cafe", "ca phe" -> List.of("coffee", "cafe", "ca phe");
      case "nha nghi", "khach san", "hotel", "homestay" -> List.of(normalized, "luu tru");
      default -> List.of(keyword);
    };
  }

  private static String pattern(String keyword) { return SearchText.pattern(keyword); }

  private static boolean present(Collection<?> values) {
    return values != null && !values.isEmpty();
  }
}
