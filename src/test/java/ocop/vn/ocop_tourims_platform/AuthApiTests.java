package ocop.vn.ocop_tourims_platform;

import ocop.vn.ocop_tourims_platform.entity.User;
import ocop.vn.ocop_tourims_platform.repository.UserRepository;
import ocop.vn.ocop_tourims_platform.service.JWTService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<String> emails = new ArrayList<>();
    private final List<String> accessTokens = new ArrayList<>();

    @AfterEach
    void cleanup() {
        accessTokens.forEach(jwtService::logout);
        for (String email : emails) {
            users.findByEmailIgnoreCase(email).ifPresent(user -> users.deleteById(user.getId()));
        }
    }

    @Test
    void registerLoginRefreshAndLogoutThroughHttp() throws Exception {
        String email = newEmail();
        Reply registration = post("register", Map.of("email", "  " + email.toUpperCase() + "  ",
                "password", "TestPassword123!", "displayName", "Khách kiểm thử"), null);
        assertEquals(201, registration.status(), registration.body().toString());
        assertEquals(email, registration.body().path("data").path("email").asText());
        assertEquals("CUSTOMER", registration.body().path("data").path("roles").get(0).asText());
        assertFalse(registration.body().path("data").has("passwordHash"));
        assertFalse(registration.body().path("data").path("userId").asText().isBlank());
        User stored = users.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(encoder.matches("TestPassword123!", stored.getPasswordHash()));

        assertEquals(409, post("register", Map.of("email", email,
                "password", "TestPassword123!", "displayName", "Trùng email"), null).status());
        Reply wrongPassword = post("login", Map.of("email", email, "password", "wrong"), null);
        assertEquals(401, wrongPassword.status());
        assertEquals("Email hoặc mật khẩu không chính xác", wrongPassword.body().path("message").asText());

        JsonNode login = post("login", Map.of("email", email, "password", "TestPassword123!"), null)
                .requireStatus(200).body().path("data");
        String access = login.path("accessToken").asText();
        String refresh = login.path("refreshToken").asText();
        accessTokens.add(access);
        assertTrue(login.path("expiresIn").asLong() > 0);
        assertNotNull(users.findByEmailIgnoreCase(email).orElseThrow().getLastLoginAt());
        assertEquals(401, post("refresh", Map.of("refreshToken", access), null).status());

        jdbc.update("DELETE FROM user_roles WHERE user_ref_id = ?", stored.getId());
        JsonNode refreshed = post("refresh", Map.of("refreshToken", refresh), null)
                .requireStatus(200).body().path("data");
        String nextAccess = refreshed.path("accessToken").asText();
        String nextRefresh = refreshed.path("refreshToken").asText();
        assertTrue(refreshed.path("user").path("roles").isEmpty());
        assertTrue(jwtService.verifyAccessToken(nextAccess).getClaimAsStringList("roles").isEmpty());
        assertNotEquals(refresh, nextRefresh);
        assertEquals(401, post("refresh", Map.of("refreshToken", refresh), null).status());

        assertEquals(200, post("logout", null, nextAccess).status());
        assertEquals(401, post("logout", null, access).status());
        assertEquals(401, post("refresh", Map.of("refreshToken", nextRefresh), null).status());
    }

    @Test
    void inactiveUsersAndMalformedRequestsReceiveVietnameseErrors() throws Exception {
        assertEquals(400, post("register", Map.of("email", "sai", "password", "1",
                "displayName", ""), null).status());
        assertEquals(400, post("refresh", Map.of(), null).status());
        Reply unauthorized = post("logout", null, null);
        assertEquals(401, unauthorized.status());
        assertEquals("Bạn cần đăng nhập để thực hiện yêu cầu",
                unauthorized.body().path("message").asText());
        assertEquals("/api/v1/auths/logout", unauthorized.body().path("path").asText());

        String email = newEmail();
        post("register", Map.of("email", email, "password", "TestPassword123!",
                "displayName", "Khách kiểm thử"), null).requireStatus(201);
        JsonNode data = post("login", Map.of("email", email, "password", "TestPassword123!"), null)
                .requireStatus(200).body().path("data");
        accessTokens.add(data.path("accessToken").asText());
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.setStatus(User.Status.UNACTIVE);
        users.saveAndFlush(user);
        assertEquals(403, post("login", Map.of("email", email, "password", "TestPassword123!"), null).status());
        assertEquals(403, post("refresh", Map.of("refreshToken",
                data.path("refreshToken").asText()), null).status());
    }

    @Test
    void concurrentRegistrationCannotCreateDuplicateEmail() throws Exception {
        String email = newEmail();
        Map<String, String> request = Map.of("email", email,
                "password", "TestPassword123!", "displayName", "Đăng ký đồng thời");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.<java.util.concurrent.Callable<Reply>>of(
                    () -> post("register", request, null),
                    () -> post("register", request, null));
            List<Integer> statuses = new ArrayList<>();
            for (var future : executor.invokeAll(tasks)) {
                statuses.add(future.get().status());
            }
            statuses.sort(Integer::compareTo);
            assertEquals(List.of(201, 409), statuses);
        }
    }

    private String newEmail() {
        String email = "auth-test-" + UUID.randomUUID() + "@example.com";
        emails.add(email);
        return email;
    }

    private Reply post(String path, Object body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auths/" + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.POST(body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), mapper.readTree(response.body()));
    }

    private record Reply(int status, JsonNode body) {
        Reply requireStatus(int expected) {
            assertEquals(expected, status, body.toString());
            return this;
        }
    }
}
