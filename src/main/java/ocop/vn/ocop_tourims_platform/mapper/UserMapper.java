package ocop.vn.ocop_tourims_platform.mapper;

import ocop.vn.ocop_tourims_platform.dto.request.RegisterRequest;
import ocop.vn.ocop_tourims_platform.dto.response.UserResponse;
import ocop.vn.ocop_tourims_platform.entity.User;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "displayName", source = "displayName")
    User toEntity(RegisterRequest request);

    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user, List<String> roles);
}
