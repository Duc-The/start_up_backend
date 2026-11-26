package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.repository.UserRepository;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagementAccess {
  private final UserRepository users;
  private final BusinessManagementRepository businesses;
  private final BusinessMemberManagementRepository members;

  public UUID actor() {
    return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
  }

  public User current() {
    User u =
        users.findById(actor()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    if (u.getStatus() != User.Status.ACTIVE)
      throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
    if (u.getUserRoles().stream()
        .noneMatch(r -> Set.of("ADMIN", "PARTNER").contains(r.getRole().getCode()))) denied();
    return u;
  }

  public boolean admin() {
    return current().getUserRoles().stream().anyMatch(r -> r.getRole().getCode().equals("ADMIN"));
  }

  public void requireAdmin() {
    if (!admin()) denied();
  }

  public Set<UUID> businessIds() {
    current();
    return new HashSet<>(
        members
            .findAll(
                (r, q, b) ->
                    b.and(
                        b.equal(r.get("user").get("id"), actor()),
                        b.isTrue(r.get("business").get("active"))))
            .stream()
            .map(m -> m.getBusiness().getId())
            .toList());
  }

  public Business business(UUID id) {
    if (id == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    Business business =
        businesses
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    if (!admin() && (!business.isActive() || !businessIds().contains(id))) denied();
    return business;
  }

  public void content(ManagedContent content) {
    if (admin()) return;
    if (content.getBusiness() == null) denied();
    business(content.getBusiness().getId());
  }

  public void stamp(BaseEntity entity, boolean created) {
    if (created) entity.setCreatedBy(actor());
    entity.setUpdatedBy(actor());
  }

  public static void denied() {
    throw new BusinessException(ErrorCode.ACCESS_DENIED);
  }

  public static void invalid(String message) {
    throw new IllegalArgumentException(message);
  }

  public static void version(ManagedContent entity, Long expected) {
    if (expected == null || entity.getVersion() != expected)
      throw new BusinessException(ErrorCode.DATA_CONFLICT);
  }
}
