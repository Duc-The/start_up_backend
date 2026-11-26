package ocop.vn.ocop_tourims_platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.dto.request.*;
import ocop.vn.ocop_tourims_platform.dto.response.*;
import ocop.vn.ocop_tourims_platform.service.AuthService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest data, HttpServletRequest request) {
        UserResponse user = authService.register(data);
        ApiResponse<UserResponse> body = ApiResponse.success(
                HttpStatus.CREATED, "Đăng ký tài khoản thành công", user, request.getRequestURI());
        return ResponseEntity.status(body.status()).cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest data, HttpServletRequest request) {
        AuthResponse result = authService.login(data);
        ApiResponse<AuthResponse> body = ApiResponse.success(
                HttpStatus.OK, "Đăng nhập thành công", result, request.getRequestURI());
        return ResponseEntity.status(body.status()).cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest data, HttpServletRequest request) {
        AuthResponse result = authService.refresh(data);
        ApiResponse<AuthResponse> body = ApiResponse.success(
                HttpStatus.OK, "Làm mới token thành công", result, request.getRequestURI());
        return ResponseEntity.status(body.status()).cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        authService.logout(jwt.getTokenValue());
        ApiResponse<Void> body = ApiResponse.success(
                HttpStatus.OK, "Đăng xuất thành công", null, request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }
}
