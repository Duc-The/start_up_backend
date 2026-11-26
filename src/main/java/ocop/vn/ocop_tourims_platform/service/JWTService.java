package ocop.vn.ocop_tourims_platform.service;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface JWTService {

  TokenPair createSession(UUID userId, Collection<String> roleCodes);

  TokenPair refresh(String refreshToken);

  TokenPair refresh(String refreshToken, Collection<String> roleCodes);

  void logout(String accessToken);

  void revokeSessions(UUID userId);

  record TokenPair(String accessToken, String refreshToken) {}

  String generateAccessToken(UUID userId, Collection<String> roleCodes);

  String generateRefreshToken(UUID userId);

  Jwt verifyAccessToken(String token);

  Jwt verifyRefreshToken(String token);
}
