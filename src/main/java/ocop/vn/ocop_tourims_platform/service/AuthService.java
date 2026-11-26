package ocop.vn.ocop_tourims_platform.service;

import ocop.vn.ocop_tourims_platform.dto.request.*;
import ocop.vn.ocop_tourims_platform.dto.response.*;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String accessToken);
}
