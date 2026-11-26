package ocop.vn.ocop_tourims_platform.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import ocop.vn.ocop_tourims_platform.service.JWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class JWTServiceImpl implements JWTService {

  private final StringRedisTemplate redis;

  private static final DefaultRedisScript<Long> ROTATE =
      new DefaultRedisScript<>(
          "if redis.call('GET', KEYS[1]) == ARGV[1] and (redis.call('GET', KEYS[2]) or '0') =="
              + " ARGV[4] then redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]); return 1 else"
              + " return 0 end",
          Long.class);

  private final byte[] accessKey;
  private final byte[] refreshKey;
  private final Duration accessTtl;
  private final Duration refreshTtl;
  private final String issuer;

  public JWTServiceImpl(
      StringRedisTemplate redis,
      @Value("${secret-key.access-key}") String accessKey,
      @Value("${secret-key.refresh-key}") String refreshKey,
      @Value("${jwt.access-ttl:PT15M}") Duration accessTtl,
      @Value("${jwt.refresh-ttl:P7D}") Duration refreshTtl,
      @Value("${jwt.issuer:ocop-tourism-platform}") String issuer) {
    this.redis = redis;
    this.accessKey = validateKey(accessKey);
    this.refreshKey = validateKey(refreshKey);
    if (accessKey.equals(refreshKey)) {
      throw new IllegalArgumentException("Khóa ký access token và refresh token phải khác nhau");
    }
    if (accessTtl.isNegative()
        || accessTtl.isZero()
        || refreshTtl.isNegative()
        || refreshTtl.isZero()
        || refreshTtl.compareTo(accessTtl) < 0
        || issuer == null
        || issuer.isBlank()) {
      throw new IllegalArgumentException(
          "Thời hạn token phải hợp lệ và phải cấu hình đơn vị phát hành");
    }
    this.accessTtl = accessTtl;
    this.refreshTtl = refreshTtl;
    this.issuer = issuer;
  }

  @Override
  public String generateAccessToken(UUID userId, Collection<String> roleCodes) {
    String sid = UUID.randomUUID().toString();
    String token =
        generateToken(userId, "access", accessKey, accessTtl, List.copyOf(roleCodes), sid);
    redis.opsForValue().set(sessionKey(sid), userId + ":access-only", accessTtl);
    return token;
  }

  @Override
  public String generateRefreshToken(UUID userId) {
    return createSession(userId, List.of()).refreshToken();
  }

  @Override
  public Jwt verifyAccessToken(String token) {
    Jwt jwt = verifyToken(token, "access", accessKey);
    requireSession(jwt, false);
    return jwt;
  }

  @Override
  public Jwt verifyRefreshToken(String token) {
    Jwt jwt = verifyToken(token, "refresh", refreshKey);
    requireSession(jwt, true);
    return jwt;
  }

  private String generateToken(
      UUID userId, String type, byte[] key, Duration ttl, List<String> roleCodes, String sid) {
    Objects.requireNonNull(userId, "userId");
    Instant now = Instant.now();
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issuer(issuer)
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ttl)))
            .jwtID(UUID.randomUUID().toString())
            .claim("token_type", type)
            .claim("user_version", userVersion(userId.toString()))
            .claim("sid", sid);
    claims.claim("roles", roleCodes);

    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
            claims.build());
    try {
      jwt.sign(new MACSigner(key));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Không thể tạo chữ ký token", exception);
    }
  }

  private Jwt verifyToken(String token, String expectedType, byte[] key) {
    if (token == null || token.isBlank()) {
      throw new BadJwtException("Token không được để trống");
    }
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
          || !JOSEObjectType.JWT.equals(jwt.getHeader().getType())
          || !jwt.verify(new MACVerifier(key))) {
        throw new BadJwtException("Chữ ký hoặc thông tin đầu token không hợp lệ");
      }

      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Instant now = Instant.now();
      Date issuedAt = claims.getIssueTime();
      Date expiresAt = claims.getExpirationTime();
      Date notBefore = claims.getNotBeforeTime();
      if (!issuer.equals(claims.getIssuer())
          || !expectedType.equals(claims.getStringClaim("token_type"))
          || issuedAt == null
          || expiresAt == null
          || notBefore == null
          || issuedAt.toInstant().isAfter(now)
          || notBefore.toInstant().isAfter(now)
          || !expiresAt.toInstant().isAfter(now)
          || !expiresAt.after(issuedAt)
          || claims.getSubject() == null
          || claims.getStringClaim("sid") == null
          || claims.getJWTID() == null) {
        throw new BadJwtException("Thông tin token không hợp lệ hoặc token đã hết hạn");
      }
      UUID.fromString(claims.getStringClaim("sid"));
      UUID.fromString(claims.getSubject());
      UUID.fromString(claims.getJWTID());

      Jwt.Builder result =
          Jwt.withTokenValue(token)
              .headers(headers -> headers.putAll(jwt.getHeader().toJSONObject()))
              .claims(values -> values.putAll(claims.getClaims()))
              .issuedAt(issuedAt.toInstant())
              .expiresAt(expiresAt.toInstant());
      result.claim("nbf", notBefore.toInstant());
      return result.build();
    } catch (ParseException | JOSEException | IllegalArgumentException exception) {
      throw new BadJwtException("Token không hợp lệ", exception);
    }
  }

  @Override
  public TokenPair createSession(UUID userId, Collection<String> roleCodes) {
    String sid = UUID.randomUUID().toString();
    TokenPair tokens = issuePair(userId, List.copyOf(roleCodes), sid);
    redis
        .opsForValue()
        .set(sessionKey(sid), sessionValue(userId.toString(), tokens.refreshToken()), refreshTtl);
    return tokens;
  }

  @Override
  public TokenPair refresh(String refreshToken) {
    Jwt old = verifyToken(refreshToken, "refresh", refreshKey);
    requireSession(old, true);
    return refresh(refreshToken, old.getClaimAsStringList("roles"));
  }

  @Override
  public TokenPair refresh(String refreshToken, Collection<String> roleCodes) {
    Jwt old = verifyToken(refreshToken, "refresh", refreshKey);
    String sid = old.getClaimAsString("sid");
    TokenPair tokens = issuePair(UUID.fromString(old.getSubject()), List.copyOf(roleCodes), sid);
    Long rotated =
        redis.execute(
            ROTATE,
            List.of(sessionKey(sid), userVersionKey(old.getSubject())),
            sessionValue(old.getSubject(), refreshToken),
            sessionValue(old.getSubject(), tokens.refreshToken()),
            Long.toString(refreshTtl.toMillis()),
            tokenUserVersion(old));
    if (!Long.valueOf(1).equals(rotated)) {
      throw new BadJwtException("Refresh token đã hết hạn, bị thu hồi hoặc đã được sử dụng");
    }
    return tokens;
  }

  @Override
  public void logout(String accessToken) {
    Jwt jwt = verifyToken(accessToken, "access", accessKey);
    redis.delete(sessionKey(jwt.getClaimAsString("sid")));
  }

  @Override
  public void revokeSessions(UUID userId) {
    redis.opsForValue().increment(userVersionKey(userId.toString()));
  }

  private String userVersion(String userId) {
    String value = redis.opsForValue().get(userVersionKey(userId));
    return value == null ? "0" : value;
  }

  private static String userVersionKey(String userId) {
    return "auth:user-version:" + userId;
  }

  private static String tokenUserVersion(Jwt jwt) {
    String value = jwt.getClaimAsString("user_version");
    return value == null ? "0" : value;
  }

  private TokenPair issuePair(UUID userId, List<String> roles, String sid) {
    return new TokenPair(
        generateToken(userId, "access", accessKey, accessTtl, roles, sid),
        generateToken(userId, "refresh", refreshKey, refreshTtl, roles, sid));
  }

  private void requireSession(Jwt jwt, boolean refresh) {
    String value;
    try {
      var values =
          redis
              .opsForValue()
              .multiGet(
                  List.of(
                      sessionKey(jwt.getClaimAsString("sid")), userVersionKey(jwt.getSubject())));
      value = values == null ? null : values.get(0);
      String currentVersion = values == null || values.get(1) == null ? "0" : values.get(1);
      if (!tokenUserVersion(jwt).equals(currentVersion))
        throw new BadJwtException("Các phiên đăng nhập của tài khoản đã bị thu hồi");
    } catch (DataAccessException exception) {
      throw new BadJwtException("Không thể xác minh phiên đăng nhập", exception);
    }
    if (value == null
        || !value.startsWith(jwt.getSubject() + ":")
        || (refresh && !value.equals(sessionValue(jwt.getSubject(), jwt.getTokenValue())))) {
      throw new BadJwtException("Phiên đăng nhập hoặc refresh token không còn hiệu lực");
    }
  }

  private static String sessionKey(String sid) {
    return "auth:session:" + sid;
  }

  private static String sessionValue(String subject, String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return subject + ":" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Không thể sử dụng thuật toán SHA-256", exception);
    }
  }

  private static byte[] validateKey(String key) {
    byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalArgumentException("Khóa ký HS256 phải có ít nhất 32 byte UTF-8");
    }
    return bytes;
  }
}
