package ocop.vn.ocop_tourims_platform.dto.response;

import java.util.List;
import java.util.UUID;
import ocop.vn.ocop_tourims_platform.entity.User;

public record UserResponse(
        UUID id,
        String userId,
        String email,
        String displayName,
        String avatarUrl,
        User.Status status,
        List<String> roles
) {
}
