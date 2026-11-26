package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import ocop.vn.ocop_tourims_platform.service.impl.JWTServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;

class JWTServiceTests {

  @Test
  void accountRevocationInvalidatesAllDevicesAndAllowsNewSessions() {
    UUID user = UUID.randomUUID();
    var first = service.createSession(user, List.of("CUSTOMER"));
    var second = service.createSession(user, List.of("CUSTOMER"));
    service.revokeSessions(user);
    assertThrows(BadJwtException.class, () -> service.verifyAccessToken(first.accessToken()));
    assertThrows(BadJwtException.class, () -> service.verifyRefreshToken(second.refreshToken()));
    assertThrows(
        BadJwtException.class, () -> service.refresh(first.refreshToken(), List.of("CUSTOMER")));
    var fresh = service.createSession(user, List.of("CUSTOMER"));
    service.verifyAccessToken(fresh.accessToken());
    service.logout(fresh.accessToken());
  }

  private LettuceConnectionFactory connection;
  private JWTServiceImpl service;

  @BeforeEach
  void connectRedis() {
    connection = new LettuceConnectionFactory("localhost", 6379);
    connection.afterPropertiesSet();
    StringRedisTemplate redis = new StringRedisTemplate(connection);
    service =
        new JWTServiceImpl(
            redis,
            "a".repeat(32),
            "b".repeat(32),
            Duration.ofMinutes(15),
            Duration.ofDays(7),
            "test-issuer");
  }

  @AfterEach
  void disconnectRedis() {
    connection.destroy();
  }

  @Test
  void rotatesRefreshTokenAndLogoutRevokesAllAccessTokensInSession() {
    UUID user = UUID.randomUUID();
    var first = service.createSession(user, List.of("CUSTOMER"));
    try {
      assertEquals(user.toString(), service.verifyAccessToken(first.accessToken()).getSubject());
      assertEquals(
          List.of("CUSTOMER"),
          service.verifyAccessToken(first.accessToken()).getClaimAsStringList("roles"));
      assertThrows(BadJwtException.class, () -> service.verifyAccessToken(first.refreshToken()));
      var second = service.refresh(first.refreshToken());
      assertThrows(BadJwtException.class, () -> service.verifyRefreshToken(first.refreshToken()));
      assertThrows(BadJwtException.class, () -> service.refresh(first.refreshToken()));
      service.verifyAccessToken(first.accessToken());
      service.verifyRefreshToken(second.refreshToken());
      service.logout(first.accessToken());
      assertThrows(BadJwtException.class, () -> service.verifyAccessToken(first.accessToken()));
      assertThrows(BadJwtException.class, () -> service.verifyAccessToken(second.accessToken()));
      assertThrows(BadJwtException.class, () -> service.refresh(second.refreshToken()));
    } finally {
      service.logout(first.accessToken());
    }
  }

  @Test
  void concurrentRefreshHasOnlyOneWinnerAndOtherDeviceRemainsActive() throws Exception {
    UUID user = UUID.randomUUID();
    var first = service.createSession(user, List.of());
    var other = service.createSession(user, List.of());
    try (var executor = Executors.newFixedThreadPool(2)) {
      var tasks =
          List.<java.util.concurrent.Callable<Boolean>>of(
              () -> tryRefresh(first.refreshToken()), () -> tryRefresh(first.refreshToken()));
      var results = executor.invokeAll(tasks);
      int successes = 0;
      for (var result : results) {
        if (result.get()) {
          successes++;
        }
      }
      assertEquals(1, successes);
      service.logout(first.accessToken());
      service.verifyAccessToken(other.accessToken());
      service.verifyRefreshToken(other.refreshToken());
      assertThrows(BadJwtException.class, () -> service.verifyAccessToken("broken"));
    } finally {
      service.logout(first.accessToken());
      service.logout(other.accessToken());
    }
  }

  private boolean tryRefresh(String token) {
    try {
      service.refresh(token);
      return true;
    } catch (BadJwtException exception) {
      return false;
    }
  }
}
