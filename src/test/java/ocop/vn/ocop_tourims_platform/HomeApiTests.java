package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.*;
import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.service.JWTService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeApiTests {
  @Value("${local.server.port}")
  int port;

  @Autowired TransactionTemplate transactions;
  @PersistenceContext EntityManager em;
  @Autowired JWTService jwt;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
  UUID poiId,
      areaId,
      activityId,
      amenityId,
      rootId,
      noticeId,
      userId,
      otherId,
      productId,
      categoryId;
  String token, otherToken;
  final List<UUID> catalogIds = new ArrayList<>();

  @BeforeEach
  void setup() {
    transactions.executeWithoutResult(
        status -> {
          String suffix = UUID.randomUUID().toString().replace("-", "");
          Area area = new Area();
          area.setName("Khu vực thử");
          area.setCode("A" + suffix.toUpperCase());
          em.persist(area);
          areaId = area.getId();
          PoiCategory category = new PoiCategory();
          category.setName("Điểm thử");
          em.persist(category);
          categoryId = category.getId();
          Activity activity = new Activity();
          activity.setName("Trải nghiệm");
          activity.setCode("A" + suffix.toUpperCase());
          activity.setIconUrl("/icon");
          em.persist(activity);
          activityId = activity.getId();
          Amenity amenity = new Amenity();
          amenity.setName("Bãi xe");
          amenity.setCode("A" + suffix.toUpperCase());
          amenity.setIconUrl("/icon");
          em.persist(amenity);
          amenityId = amenity.getId();
          ProductType root = new ProductType();
          root.setName("Thực phẩm");
          root.setSlug("root-" + suffix);
          root.setIconUrl("/icon");
          em.persist(root);
          rootId = root.getId();
          ProductType leaf = new ProductType();
          leaf.setName("Nước mắm");
          leaf.setSlug("leaf-" + suffix);
          leaf.setIconUrl("/icon");
          leaf.setParent(root);
          em.persist(leaf);
          catalogIds.add(leaf.getId());
          Poi poi = new Poi();
          poi.setName("Điểm thử " + suffix);
          poi.setAddress("Đà Nẵng");
          poi.setArea(area);
          poi.setCoordinates(16, 108);
          poi.addCategory(category);
          poi.addActivity(activity);
          poi.addAmenity(amenity);
          poi.addMedia(PoiMedia.Type.IMAGE, "/image", 0);
          em.persist(poi);
          poiId = poi.getId();
          Product product = new Product();
          product.setName("Nước mắm " + suffix);
          product.setDescription("Mô tả");
          product.setProductType(leaf);
          product.setManufacturerPoi(poi);
          em.persist(product);
          productId = product.getId();
          ProductListing listing = new ProductListing();
          listing.setProduct(product);
          listing.setPoi(poi);
          listing.setPriceVnd(10000);
          listing.setStockQuantity(10);
          em.persist(listing);
          Certification cert = new Certification(product, Certification.Type.OCOP, (short) 4);
          cert.setName("Chứng nhận");
          cert.setCertificateNumber(suffix);
          cert.setIssuingAuthority("Cơ quan thử");
          cert.setIssuedOn(LocalDate.now().minusDays(1));
          cert.setExpiresOn(LocalDate.now().plusDays(1));
          em.persist(cert);
          User user = new User();
          user.setEmail(suffix + "@example.com");
          user.setDisplayName("Khách thử");
          em.persist(user);
          userId = user.getId();
          User other = new User();
          other.setEmail("other" + suffix + "@example.com");
          other.setDisplayName("Khách khác");
          em.persist(other);
          otherId = other.getId();
          Notification notice = new Notification(user, "Thông báo", "Nội dung", "/pois/" + poiId);
          em.persist(notice);
          noticeId = notice.getId();
        });
    token = jwt.createSession(userId, List.of()).accessToken();
    otherToken = jwt.createSession(otherId, List.of()).accessToken();
  }

  @AfterEach
  void cleanup() {
    jwt.logout(token);
    jwt.logout(otherToken);
    redis.delete(
        List.of(
            "home:v1:filters",
            "home:v1:poi:" + poiId,
            "home:v2:suggestions:10:"
                + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString("nước mắm".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    transactions.executeWithoutResult(
        status -> {
          em.createNativeQuery("delete from certifications where product_ref_id=:id")
              .setParameter("id", productId)
              .executeUpdate();
          em.createNativeQuery("delete from product_listings where product_ref_id=:id")
              .setParameter("id", productId)
              .executeUpdate();
          em.remove(em.find(Product.class, productId));
          em.flush();
          em.remove(em.find(Poi.class, poiId));
          em.flush();
          em.remove(em.find(ProductType.class, catalogIds.getFirst()));
          em.flush();
          em.remove(em.find(ProductType.class, rootId));
          em.remove(em.find(PoiCategory.class, categoryId));
          em.remove(em.find(Activity.class, activityId));
          em.remove(em.find(Amenity.class, amenityId));
          em.remove(em.find(Area.class, areaId));
          em.remove(em.find(User.class, userId));
          em.remove(em.find(User.class, otherId));
        });
  }

  @Test
  void homeEndpointsWorkWithFiltersAndOwnership() throws Exception {
    call("GET", "/home/filters", null, 200);
    String filter =
        "?areaIds="
            + areaId
            + "&productTypeIds="
            + rootId
            + "&categoryIds="
            + categoryId
            + "&activityIds="
            + activityId
            + "&amenityIds="
            + amenityId
            + "&ocopStars=4&lat=16&lng=108&radiusMeters=2000&sort=distance";
    JsonNode result = call("GET", "/pois" + filter, null, 200);
    assertEquals(1, result.path("data").path("total").asInt());
    assertEquals(poiId.toString(), result.path("data").path("items").get(0).path("id").asText());
    call("GET", "/pois/map" + filter + "&minLat=15&maxLat=17&minLng=107&maxLng=109", null, 200);
    call("GET", "/pois?sort=distance", null, 400);
    call("GET", "/pois?areaIds=" + areaId + "&lat=16&lng=108&sort=distance", null, 200);
    call(
        "GET",
        "/pois?areaIds="
            + areaId
            + "&keyword="
            + URLEncoder.encode("Nước mắm", java.nio.charset.StandardCharsets.UTF_8),
        null,
        200);
    call("GET", "/pois/" + UUID.randomUUID(), null, 404);
    call("GET", "/pois?lat=NaN&lng=108", null, 400);
    call("GET", "/pois/" + poiId, null, 200);
    call("GET", "/pois/" + poiId, null, 200);
    assertEquals(
        1,
        call("GET", "/pois/" + poiId + "/products", null, 200).path("data").path("total").asInt());
    call("GET", "/pois/" + poiId + "/reviews", null, 200);
    call(
        "GET",
        "/search/suggestions?keyword="
            + URLEncoder.encode("Nước mắm", java.nio.charset.StandardCharsets.UTF_8),
        null,
        200);
    var invalidRouteRequest = HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/pois/directions"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"points\":[],\"mode\":\"flying\"}")).build();
    assertEquals(400, HttpClient.newHttpClient().send(invalidRouteRequest, HttpResponse.BodyHandlers.ofString()).statusCode());
    var noAccentSuggestions=call("GET", "/search/suggestions?keyword=nuoc%20mam", null, 200).path("data");
    assertFalse(noAccentSuggestions.isEmpty());
    call("GET", "/search/suggestions?keyword=x&limit=0", null, 400);
    call("GET", "/me", null, 401);
    call("GET", "/me", token, 200);
    call("PUT", "/me/favorite-pois/" + poiId, token, 200);
    call("PUT", "/me/favorite-pois/" + poiId, token, 200);
    assertEquals(
        1, call("GET", "/me/favorite-pois", token, 200).path("data").path("total").asInt());
    assertTrue(
        call("GET", "/pois/" + poiId, token, 200)
            .path("data")
            .path("poi")
            .path("favorite")
            .asBoolean());
    assertFalse(
        call("GET", "/pois/" + poiId, otherToken, 200)
            .path("data")
            .path("poi")
            .path("favorite")
            .asBoolean());
    call("DELETE", "/me/favorite-pois/" + poiId, token, 200);
    call("GET", "/me/notifications", token, 200);
    assertEquals(1, call("GET", "/me/notifications/unread-count", token, 200).path("data").asInt());
    call("PATCH", "/me/notifications/" + noticeId + "/read", otherToken, 404);
    call("PATCH", "/me/notifications/" + noticeId + "/read", token, 200);
    call("PATCH", "/me/notifications/read-all", token, 200);
    assertEquals(0, call("GET", "/me/notifications/unread-count", token, 200).path("data").asInt());
  }

  private JsonNode call(String method, String path, String access, int expected) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
            .method(method, HttpRequest.BodyPublishers.noBody());
    if (access != null) request.header("Authorization", "Bearer " + access);
    var response =
        HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(expected, response.statusCode(), response.body());
    return json.readTree(response.body());
  }
}
