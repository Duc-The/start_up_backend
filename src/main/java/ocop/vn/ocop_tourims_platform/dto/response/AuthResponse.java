package ocop.vn.ocop_tourims_platform.dto.response;

public record AuthResponse(
        UserResponse user,
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
