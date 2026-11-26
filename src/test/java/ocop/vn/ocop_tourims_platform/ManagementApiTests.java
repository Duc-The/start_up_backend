package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.entity.Order;
import ocop.vn.ocop_tourims_platform.repository.RoleRepository;
import ocop.vn.ocop_tourims_platform.service.JWTService;
import ocop.vn.ocop_tourims_platform.service.management.CloudinaryGateway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ManagementApiTests {
  @Value("${local.server.port}")
  int port;

  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;
  @Autowired JWTService jwt;
  @Autowired RoleRepository roles;
  @Autowired ObjectMapper json;
  @MockitoBean CloudinaryGateway cloud;
  UUID adminId, partnerId, otherId, customerId, businessId, otherBusinessId, areaId, typeId;
  String admin, partner, other, customer;
  String suffix;
  final HttpClient client = HttpClient.newHttpClient();

  record Reply(int status, JsonNode body) {
    JsonNode data() {
      return body.path("data");
    }
  }

  @BeforeEach
  void setup() {
    suffix = UUID.randomUUID().toString().replace("-", "");
    tx.executeWithoutResult(
        s -> {
          adminId = user("admin", "ADMIN").getId();
          partnerId = user("partner", "PARTNER").getId();
          otherId = user("other", "PARTNER").getId();
          customerId = user("customer", "CUSTOMER").getId();
          Business b = new Business();
          b.setName("Cơ sở thử " + suffix);
          b.setCreatedBy(adminId);
          em.persist(b);
          businessId = b.getId();
          Business second = new Business();
          second.setName("Cơ sở khác " + suffix);
          second.setCreatedBy(adminId);
          em.persist(second);
          otherBusinessId = second.getId();
          member(b, partnerId);
          member(second, otherId);
          Area a = new Area();
          a.setCode("TEST" + suffix.toUpperCase());
          a.setName("Khu vực thử");
          em.persist(a);
          areaId = a.getId();
          ProductType t = new ProductType();
          t.setName("Nhóm thử");
          t.setSlug("test-" + suffix);
          t.setIconUrl("/test-icon");
          em.persist(t);
          typeId = t.getId();
        });
    admin = jwt.createSession(adminId, List.of("ADMIN")).accessToken();
    partner = jwt.createSession(partnerId, List.of("PARTNER")).accessToken();
    other = jwt.createSession(otherId, List.of("PARTNER")).accessToken();
    customer = jwt.createSession(customerId, List.of("CUSTOMER")).accessToken();
  }

  private User user(String name, String role) {
    User u = new User();
    u.setEmail(name + suffix + "@test.local");
    u.setDisplayName(name);
    u.addRole(roles.findByCode(role).orElseThrow(), null);
    em.persist(u);
    return u;
  }

  private void member(Business b, UUID userId) {
    var m = new BusinessMember();
    m.setBusiness(b);
    m.setUser(em.getReference(User.class, userId));
    m.setCreatedBy(adminId);
    em.persist(m);
  }

  @AfterEach
  void clean() {
    tx.executeWithoutResult(
        s -> {
          var actors = List.of(adminId, partnerId, otherId, customerId);
          em.createNativeQuery(
                  "delete from order_items where order_ref_id in (select id from orders where"
                      + " created_by in (:ids))")
              .setParameter("ids", actors)
              .executeUpdate();
          for (String table :
              List.of(
                  "media_links",
                  "product_images",
                  "poi_media",
                  "certifications",
                  "stories",
                  "tourism_events",
                  "tour_route_segments",
                  "tour_stops",
                  "tours",
                  "poi_reviews",
                  "product_reviews",
                  "order_items",
                  "payments",
                  "orders",
                  "product_listings",
                  "products",
                  "poi_category_links",
                  "poi_activities",
                  "poi_amenities",
                  "poi_geometries",
                  "pois",
                  "business_members",
                  "media_assets",
                  "businesses",
                  "management_audits",
                  "import_batches"))
            em.createNativeQuery("delete from " + table + " where created_by in (:ids)")
                .setParameter("ids", actors)
                .executeUpdate();
          // Geometry and owned collection rows are generated through aggregate methods.
          em.createNativeQuery("delete from product_types where id=:id")
              .setParameter("id", typeId)
              .executeUpdate();
          em.createNativeQuery("delete from areas where created_by in (:ids)")
              .setParameter("ids", actors)
              .executeUpdate();
          em.createNativeQuery("delete from areas where id=:id")
              .setParameter("id", areaId)
              .executeUpdate();
          em.createNativeQuery("delete from user_roles where user_ref_id in (:ids)")
              .setParameter("ids", actors)
              .executeUpdate();
          em.createNativeQuery("delete from users where id in (:ids)")
              .setParameter("ids", actors)
              .executeUpdate();
        });
  }

  private Reply call(String method, String path, String token, Object body) throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (body != null) builder.header("Content-Type", "application/json");
    builder.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    var result = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return new Reply(result.statusCode(), json.readTree(result.body()));
  }

  private String path(String s) {
    return "/api/v1/management" + s;
  }

  private Map<String, Object> poiForm(long version) {
    var form = new HashMap<String, Object>();
    form.put("businessId", businessId);
    form.put("areaId", areaId);
    form.put("name", "Địa điểm " + suffix);
    form.put("address", "Đà Nẵng");
    form.put("lat", 16.05);
    form.put("lng", 108.2);
    form.put("openingHours", Map.of());
    form.put("categoryIds", List.of());
    form.put("activityIds", List.of());
    form.put("amenityIds", List.of());
    form.put("version", version);
    return form;
  }

  private JsonNode createPoi() throws Exception {
    var result = call("POST", path("/pois"), partner, poiForm(0));
    assertEquals(201, result.status(), result.body().toString());
    return result.data();
  }

  @Test
  void ownershipApprovalCacheAndConcurrency() throws Exception {
    assertEquals(401, call("GET", path("/dashboard"), null, null).status());
    assertEquals(403, call("GET", path("/dashboard"), customer, null).status());
    assertEquals(403, call("GET", path("/businesses/" + businessId), other, null).status());
    var poi = createPoi();
    String id = poi.path("id").asText();
    assertEquals("DRAFT", poi.path("publicationStatus").asText());
    assertEquals(404, call("GET", "/api/v1/pois/" + id, null, null).status());
    assertEquals(
        403,
        call("PUT", path("/pois/" + id), other, poiForm(poi.path("version").asLong())).status());
    var submit =
        call(
            "POST",
            path("/pois/" + id + "/submit"),
            partner,
            Map.of("version", poi.path("version").asLong()));
    assertEquals(200, submit.status(), submit.body().toString());
    assertEquals(
        403,
        call(
                "POST",
                path("/pois/" + id + "/approve"),
                partner,
                Map.of("version", submit.data().path("version").asLong()))
            .status());
    var approved =
        call(
            "POST",
            path("/pois/" + id + "/approve"),
            admin,
            Map.of("version", submit.data().path("version").asLong()));
    assertEquals(200, approved.status(), approved.body().toString());
    assertEquals(200, call("GET", "/api/v1/pois/" + id, null, null).status());
    var updated =
        call(
            "PUT", path("/pois/" + id), partner, poiForm(approved.data().path("version").asLong()));
    assertEquals(200, updated.status(), updated.body().toString());
    assertEquals(404, call("GET", "/api/v1/pois/" + id, null, null).status());
    assertEquals(
        409,
        call("PUT", path("/pois/" + id), partner, poiForm(approved.data().path("version").asLong()))
            .status());
    assertEquals(
        0,
        call("GET", path("/pois?businessId=" + otherBusinessId), other, null)
            .data()
            .path("total")
            .asLong());
    assertEquals(400, call("GET", path("/pois?size=101"), admin, null).status());
    assertEquals(200, call("GET", path("/dashboard"), partner, null).status());
  }

  @Test
  void cloudinaryOwnershipVerificationAndReferences() throws Exception {
    when(cloud.ticket(anyString(), eq("image"), anyLong()))
        .thenAnswer(
            i ->
                new CloudinaryGateway.Ticket(
                    "https://example.test/upload", "test-key", "signature"));
    var upload =
        call(
            "POST",
            path("/media/upload-signature"),
            partner,
            Map.of("businessId", businessId, "resourceType", "image", "altText", "Ảnh thử"));
    assertEquals(201, upload.status(), upload.body().toString());
    String mediaId = upload.data().path("id").asText(),
        publicId = upload.data().path("publicId").asText();
    when(cloud.inspect(publicId, "image"))
        .thenAnswer(
            i ->
                Map.of(
                    "public_id",
                    publicId,
                    "resource_type",
                    "image",
                    "format",
                    "jpg",
                    "bytes",
                    1000,
                    "secure_url",
                    "https://res.cloudinary.com/test/image/upload/test.jpg",
                    "asset_id",
                    "asset-" + suffix,
                    "width",
                    100,
                    "height",
                    100));
    assertEquals(403, call("POST", path("/media/" + mediaId + "/complete"), other, null).status());
    assertEquals(
        200, call("POST", path("/media/" + mediaId + "/complete"), partner, null).status());
    var poi = createPoi();
    String id = poi.path("id").asText();
    assertEquals(
        200,
        call(
                "PUT",
                path("/pois/" + id + "/media"),
                partner,
                Map.of("mediaIds", List.of(mediaId), "version", poi.path("version").asLong()))
            .status());
    assertEquals(409, call("DELETE", path("/media/" + mediaId), partner, null).status());
    verify(cloud, never()).destroy(anyString(), anyString());
    var current = call("GET", path("/pois/" + id), partner, null).data();
    assertEquals(
        200,
        call(
                "PUT",
                path("/pois/" + id + "/media"),
                partner,
                Map.of("mediaIds", List.of(), "version", current.path("version").asLong()))
            .status());
    assertEquals(200, call("DELETE", path("/media/" + mediaId), partner, null).status());
    verify(cloud).destroy(publicId, "image");
  }

  @Test
  void productCertificateStoryEventTourAndRoleAdministration() throws Exception {
    var poi = createPoi();
    String poiId = poi.path("id").asText();
    var product =
        call(
            "POST",
            path("/products"),
            partner,
            Map.of(
                "manufacturerPoiId",
                poiId,
                "productTypeId",
                typeId,
                "name",
                "Sản phẩm " + suffix,
                "description",
                "Giới thiệu sản phẩm"));
    assertEquals(201, product.status(), product.body().toString());
    var cert = new HashMap<String, Object>();
    cert.put("productId", product.data().path("id").asText());
    cert.put("type", "OCOP");
    cert.put("ocopStars", 4);
    cert.put("name", "OCOP");
    cert.put("certificateNumber", suffix);
    cert.put("issuingAuthority", "Đà Nẵng");
    cert.put("issuedOn", "2026-01-01");
    assertEquals(201, call("POST", path("/certifications"), partner, cert).status());
    var story =
        Map.of(
            "poiId",
            poiId,
            "name",
            "Câu chuyện " + suffix,
            "slug",
            "story-" + suffix,
            "introduction",
            "Giới thiệu",
            "founder",
            "Người sáng lập",
            "sections",
            List.of(Map.of("heading", "Quy trình", "body", "Nội dung", "mediaIds", List.of())));
    assertEquals(201, call("POST", path("/stories"), partner, story).status());
    assertEquals(
        201,
        call(
                "POST",
                path("/events"),
                partner,
                Map.of(
                    "poiId",
                    poiId,
                    "name",
                    "Sự kiện " + suffix,
                    "slug",
                    "event-" + suffix,
                    "description",
                    "Giới thiệu",
                    "startsAt",
                    "2026-12-01T00:00:00Z",
                    "endsAt",
                    "2026-12-02T00:00:00Z",
                    "organizer",
                    "Cơ sở",
                    "registrationUrl",
                    "",
                    "recurrence",
                    ""))
            .status());
    var tour =
        call(
            "POST",
            path("/tours"),
            partner,
            Map.of(
                "businessId",
                businessId,
                "name",
                "Tour " + suffix,
                "slug",
                "tour-" + suffix,
                "description",
                "Trải nghiệm"));
    assertEquals(201, tour.status(), tour.body().toString());
    String tourId = tour.data().path("id").asText();
    var stopResult =
        call(
            "PUT",
            path("/tours/" + tourId + "/stops"),
            partner,
            Map.of(
                "version",
                tour.data().path("version").asLong(),
                "stops",
                List.of(
                    Map.of("poiId", poiId, "visitDurationMinutes", 30),
                    Map.of("poiId", poiId, "visitDurationMinutes", 20))));
    assertEquals(200, stopResult.status(), stopResult.body().toString());
    var stops = call("GET", path("/tours/" + tourId + "/stops"), partner, null).data();
    assertEquals(2, stops.size());
    var route =
        Map.of(
            "fromStopId",
            stops.get(0).path("id").asText(),
            "toStopId",
            stops.get(1).path("id").asText(),
            "travelMode",
            "DRIVING",
            "pathCoordinates",
            List.of(Map.of("lat", 16, "lng", 108), Map.of("lat", 16.1, "lng", 108.1)),
            "distanceMeters",
            1000,
            "durationSeconds",
            300,
            "version",
            stopResult.data().path("version").asLong());
    assertEquals(200, call("PUT", path("/tours/" + tourId + "/routes"), partner, route).status());
    assertEquals(403, call("GET", path("/users"), partner, null).status());
    assertEquals(
        200,
        call(
                "PUT",
                path("/users/" + partnerId + "/roles"),
                admin,
                Map.of("roles", List.of("CUSTOMER")))
            .status());
    assertEquals(
        403,
        call("GET", path("/dashboard"), partner, null)
            .status()); // Previously issued JWT must not retain revoked privileges.
  }

  @Test
  void excelPreviewEditAtomicCommitAndReplay() throws Exception {
    byte[] workbook;
    try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        var out = new java.io.ByteArrayOutputStream()) {
      var sheet = wb.createSheet("Địa điểm");
      sheet.createRow(0).createCell(0).setCellValue("Tên cơ sở");
      sheet.createRow(1).createCell(0).setCellValue("Nhập thử " + suffix);
      sheet.createRow(2).createCell(0).setCellValue("Nhập thử " + suffix);
      wb.write(out);
      workbook = out.toByteArray();
    }
    var defaults = poiForm(0);
    defaults.remove("name");
    String boundary = "ocop-" + suffix;
    var out = new java.io.ByteArrayOutputStream();
    for (var part :
        Map.of("mapping", Map.of("name", "Tên cơ sở"), "defaults", defaults).entrySet()) {
      out.write(
          ("--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\""
                  + part.getKey()
                  + "\"\r\nContent-Type: application/json\r\n\r\n"
                  + json.writeValueAsString(part.getValue())
                  + "\r\n")
              .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    out.write(
        ("--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"data.xlsx\"\r\n"
                + "Content-Type:"
                + " application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    out.write(workbook);
    out.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var req =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + path("/imports/pois/preview")))
            .header("Authorization", "Bearer " + admin)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
            .build();
    var res = client.send(req, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, res.statusCode(), res.body());
    var batch = json.readTree(res.body()).path("data");
    String id = batch.path("id").asText();
    assertFalse(batch.path("rows").get(1).path("errors").isEmpty());
    assertEquals(
        400,
        call(
                "POST",
                path("/imports/" + id + "/commit"),
                admin,
                Map.of("version", batch.path("version").asLong()))
            .status());
    var data = poiForm(0);
    data.put("name", "Nhập đã sửa " + suffix);
    var edit =
        call(
            "PUT",
            path("/imports/" + id + "/rows"),
            admin,
            Map.of("rows", List.of(data), "version", batch.path("version").asLong()));
    assertEquals(200, edit.status(), edit.body().toString());
    var commit =
        call(
            "POST",
            path("/imports/" + id + "/commit"),
            admin,
            Map.of("version", edit.data().path("version").asLong()));
    assertEquals(200, commit.status(), commit.body().toString());
    assertEquals("COMMITTED", commit.data().path("status").asText());
    assertEquals(1, commit.data().path("importedIds").size());
    var again =
        call(
            "POST",
            path("/imports/" + id + "/commit"),
            admin,
            Map.of("version", edit.data().path("version").asLong()));
    assertEquals(200, again.status());
    assertEquals(commit.data().path("importedIds"), again.data().path("importedIds"));
    assertEquals(
        "DRAFT",
        call("GET", path("/pois/" + commit.data().path("importedIds").get(0).asText()), admin, null)
            .data()
            .path("publicationStatus")
            .asText());
  }

  @Test
  void splitOrdersModerationAndPaymentProtection() throws Exception {
    String poiId = createPoi().path("id").asText();
    UUID[] ids =
        tx.execute(
            s -> {
              Poi p = em.find(Poi.class, UUID.fromString(poiId));
              Poi second = new Poi();
              second.setName("Điểm bán khác " + suffix);
              second.setAddress("Đà Nẵng");
              second.setArea(em.getReference(Area.class, areaId));
              second.setBusiness(em.getReference(Business.class, otherBusinessId));
              second.setCreatedBy(adminId);
              em.persist(second);
              Product product = new Product();
              product.setName("Sản phẩm đơn hàng " + suffix);
              product.setDescription("Mô tả");
              product.setProductType(em.getReference(ProductType.class, typeId));
              product.setManufacturerPoi(p);
              product.setBusiness(p.getBusiness());
              product.setCreatedBy(adminId);
              em.persist(product);
              ProductListing l = new ProductListing();
              l.setPoi(p);
              l.setProduct(product);
              l.setPriceVnd(100000);
              l.setStockQuantity(10);
              l.setCreatedBy(adminId);
              em.persist(l);
              ProductListing otherListing = new ProductListing();
              otherListing.setPoi(second);
              otherListing.setProduct(product);
              otherListing.setPriceVnd(120000);
              otherListing.setStockQuantity(10);
              otherListing.setCreatedBy(adminId);
              em.persist(otherListing);
              Order o =
                  new Order(
                      em.getReference(User.class, customerId),
                      "Khách thử",
                      "0900000000",
                      "Đà Nẵng",
                      20000);
              o.setCreatedBy(adminId);
              o.addItem(l, 1);
              o.addItem(otherListing, 2);
              em.persist(o);
              em.flush();
              Payment payment = new Payment(o);
              payment.setCreatedBy(adminId);
              em.persist(payment);
              var review =
                  new PoiReview(em.getReference(User.class, customerId), p, 5, "Đánh giá thử");
              review.setCreatedBy(adminId);
              em.persist(review);
              em.flush();
              em.createNativeQuery("update orders set status='PAID' where id=:id")
                  .setParameter("id", o.getId())
                  .executeUpdate();
              return new UUID[] {
                o.getId(), o.getItems().get(0).getId(), o.getItems().get(1).getId(), review.getId()
              };
            });
    var visible = call("GET", path("/orders/" + ids[0]), partner, null);
    assertEquals(200, visible.status(), visible.body().toString());
    assertEquals(1, visible.data().path("items").size());
    assertTrue(
        visible.data().path("totalVnd").isNull()
            || visible.data().path("totalVnd").isMissingNode());
    assertEquals(
        403,
        call(
                "PATCH",
                path("/orders/" + ids[0] + "/items/" + ids[2] + "/fulfillment"),
                partner,
                Map.of("status", "SHIPPED"))
            .status());
    assertEquals(
        200,
        call(
                "PATCH",
                path("/orders/" + ids[0] + "/items/" + ids[1] + "/fulfillment"),
                partner,
                Map.of("status", "SHIPPED"))
            .status());
    assertEquals(
        400,
        call("PATCH", path("/orders/" + ids[0] + "/status"), admin, Map.of("status", "PAID"))
            .status());
    assertEquals(403, call("GET", path("/payments"), partner, null).status());
    assertEquals(200, call("GET", path("/payments?orderId=" + ids[0]), admin, null).status());
    assertEquals(
        403,
        call(
                "PATCH",
                path("/reviews/poi/" + ids[3]),
                partner,
                Map.of("status", "HIDDEN", "reason", "Kiểm duyệt"))
            .status());
    assertEquals(
        200,
        call(
                "PATCH",
                path("/reviews/poi/" + ids[3]),
                admin,
                Map.of("status", "HIDDEN", "reason", "Kiểm duyệt"))
            .status());
    tx.executeWithoutResult(
        s -> {
          var p = em.find(Poi.class, UUID.fromString(poiId));
          assertEquals(0, p.getRatingCount());
        });
  }

  @Test
  void catalogCyclesMembershipRevocationAndAccountLock() throws Exception {
    var rootForm = new HashMap<String, Object>();
    rootForm.put("name", "Danh mục " + suffix);
    rootForm.put("slug", "root-mgt-" + suffix);
    rootForm.put("iconUrl", "/icon");
    rootForm.put("active", true);
    rootForm.put("sortOrder", 0);
    assertEquals(403, call("POST", path("/catalogs/product-types"), partner, rootForm).status());
    var root = call("POST", path("/catalogs/product-types"), admin, rootForm);
    assertEquals(201, root.status(), root.body().toString());
    String rootId = root.data().path("id").asText();
    var childForm = new HashMap<>(rootForm);
    childForm.put("slug", "child-mgt-" + suffix);
    childForm.put("parentId", rootId);
    var child = call("POST", path("/catalogs/product-types"), admin, childForm);
    assertEquals(201, child.status());
    String childId = child.data().path("id").asText();
    rootForm.put("parentId", childId);
    assertEquals(
        400, call("PUT", path("/catalogs/product-types/" + rootId), admin, rootForm).status());
    assertEquals(
        409, call("DELETE", path("/catalogs/product-types/" + rootId), admin, null).status());
    assertEquals(
        200, call("DELETE", path("/catalogs/product-types/" + childId), admin, null).status());
    assertEquals(
        200, call("DELETE", path("/catalogs/product-types/" + rootId), admin, null).status());
    assertEquals(
        200,
        call("DELETE", path("/businesses/" + businessId + "/members/" + partnerId), admin, null)
            .status());
    assertEquals(403, call("GET", path("/businesses/" + businessId), partner, null).status());
    assertEquals(200, call("GET", "/api/v1/me", customer, null).status());
    assertEquals(
        200,
        call("PATCH", path("/users/" + customerId + "/status"), admin, Map.of("status", "UNACTIVE"))
            .status());
    assertEquals(401, call("GET", "/api/v1/me", customer, null).status());
    assertEquals(
        200,
        call("PATCH", path("/users/" + customerId + "/status"), admin, Map.of("status", "ACTIVE"))
            .status());
    assertEquals(401, call("GET", "/api/v1/me", customer, null).status());
  }

  @Test
  void acceptsAndNormalizesPastedAreaCodeOverHttp() throws Exception {
    String expected = "HAI_CHAU_" + suffix.toUpperCase(Locale.ROOT);
    var result =
        call(
            "POST",
            path("/catalogs/areas"),
            admin,
            Map.of(
                "name",
                "Hải Châu",
                "code",
                "\u00a0hai_chau_" + suffix + "\u200b ",
                "sortOrder",
                0,
                "active",
                true));
    assertEquals(201, result.status(), result.body().toString());
    assertEquals(expected, result.data().path("code").asText());
    assertEquals("Hải Châu", result.data().path("name").asText());
    var invalid =
        call(
            "POST",
            path("/catalogs/areas"),
            admin,
            Map.of(
                "name",
                "Khu vực không hợp lệ",
                "code",
                "HAI CHAU",
                "sortOrder",
                0,
                "active",
                true));
    assertEquals(400, invalid.status());
  }

  @Test
  void uploadsCatalogIconWithoutBusinessAndValidatesCloudinaryAsset() throws Exception {
    when(cloud.ticket(anyString(), eq("image"), anyLong()))
        .thenReturn(
            new CloudinaryGateway.Ticket(
                "https://api.cloudinary.com/v1_1/test/image/upload", "key", "signature"));
    assertEquals(
        403, call("POST", path("/media/catalog-images/upload-signature"), partner, null).status());
    var ticket = call("POST", path("/media/catalog-images/upload-signature"), admin, null);
    assertEquals(201, ticket.status(), ticket.body().toString());
    String publicId = ticket.data().path("publicId").asText();
    assertTrue(publicId.startsWith("ocop/catalog-icons/"));
    String url = "https://res.cloudinary.com/test/image/upload/" + publicId + ".png";
    var remote = new HashMap<String, Object>();
    remote.put("public_id", publicId);
    remote.put("resource_type", "image");
    remote.put("format", "png");
    remote.put("bytes", 2048);
    remote.put("secure_url", url);
    when(cloud.inspect(publicId, "image")).thenAnswer(invocation -> remote);
    var complete =
        call("POST", path("/media/catalog-images/complete"), admin, Map.of("publicId", publicId));
    assertEquals(200, complete.status(), complete.body().toString());
    assertEquals(url, complete.data().path("secureUrl").asText());
    assertEquals(
        403,
        call("POST", path("/media/catalog-images/complete"), partner, Map.of("publicId", publicId))
            .status());
    assertEquals(
        400,
        call(
                "POST",
                path("/media/catalog-images/complete"),
                admin,
                Map.of("publicId", "ocop/other/file"))
            .status());
    remote.put("format", "svg");
    assertEquals(
        400,
        call("POST", path("/media/catalog-images/complete"), admin, Map.of("publicId", publicId))
            .status());
    remote.put("format", "png");
    remote.put("bytes", 11L * 1024 * 1024);
    assertEquals(
        400,
        call("POST", path("/media/catalog-images/complete"), admin, Map.of("publicId", publicId))
            .status());
    remote.put("bytes", 2048);
    remote.put("secure_url", "https://example.com/image.png");
    assertEquals(
        400,
        call("POST", path("/media/catalog-images/complete"), admin, Map.of("publicId", publicId))
            .status());
  }
}
