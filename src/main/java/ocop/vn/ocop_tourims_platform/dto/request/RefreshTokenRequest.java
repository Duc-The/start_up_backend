package ocop.vn.ocop_tourims_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        @Size(max = 16384, message = "Refresh token quá dài")
        String refreshToken
) {
}
