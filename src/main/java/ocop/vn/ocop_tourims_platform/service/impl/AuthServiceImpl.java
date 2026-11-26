package ocop.vn.ocop_tourims_platform.service.impl;

import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.*;
import ocop.vn.ocop_tourims_platform.dto.response.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.UserMapper;
import ocop.vn.ocop_tourims_platform.repository.*;
import ocop.vn.ocop_tourims_platform.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        roles.ensureCustomerRole();
        Role customer = roles.findByCode("CUSTOMER")
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        User user = mapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.addRole(customer, null);
        users.saveAndFlush(user);
        return mapper.toResponse(user, roleCodes(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        requireActive(user);
        user.setLastLoginAt(Instant.now());
        users.flush();
        List<String> codes = roleCodes(user);
        return authResponse(user, codes, jwtService.createSession(user.getId(), codes));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        var token = jwtService.verifyRefreshToken(request.refreshToken());
        User user = users.findById(UUID.fromString(token.getSubject()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        requireActive(user);
        List<String> codes = roleCodes(user);
        return authResponse(user, codes, jwtService.refresh(request.refreshToken(), codes));
    }

    @Override
    public void logout(String accessToken) {
        jwtService.logout(accessToken);
    }

    private void requireActive(User user) {
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }

    private List<String> roleCodes(User user) {
        return user.getUserRoles().stream()
                .map(link -> link.getRole().getCode())
                .sorted()
                .toList();
    }

    private AuthResponse authResponse(User user, List<String> codes, JWTService.TokenPair tokens) {
        var access = jwtService.verifyAccessToken(tokens.accessToken());
        long expiresIn = Math.max(0, Duration.between(Instant.now(), access.getExpiresAt()).toSeconds());
        return new AuthResponse(mapper.toResponse(user, codes), "Bearer",
                tokens.accessToken(), tokens.refreshToken(), expiresIn);
    }
}
