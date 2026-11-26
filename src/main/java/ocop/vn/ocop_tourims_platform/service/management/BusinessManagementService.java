package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.PageData;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.Item;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import ocop.vn.ocop_tourims_platform.repository.*;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessManagementService {
  private final BusinessManagementRepository businesses;
  private final BusinessMemberManagementRepository members;
  private final UserRepository users;
  private final RoleRepository roles;
  private final ManagementAccess access;
  private final ManagementChanges changes;
  private final ManagementMapper mapper;

  public PageData<Item> list(String keyword, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> ids = admin ? Set.of() : access.businessIds();
    var result =
        businesses.findAll(
            (r, q, b) ->
                b.and(
                    admin ? b.conjunction() : r.get("id").in(ids),
                    keyword == null || keyword.isBlank()
                        ? b.conjunction()
                        : b.like(b.lower(r.get("name")), ManagementService.pattern(keyword), '!')),
            ManagementService.pageable(page, size));
    return new PageData<>(
        result.getContent().stream().map(mapper::item).toList(),
        result.getTotalElements(),
        page,
        size);
  }

  public Item detail(UUID id) {
    return mapper.item(access.business(id));
  }

  @Transactional
  public Item save(UUID id, BusinessForm f) {
    boolean created = id == null;
    if (created) access.requireAdmin();
    Business b = created ? new Business() : access.business(id);
    if (!created && (f.version() == null || f.version() != b.getVersion()))
      throw new BusinessException(ErrorCode.DATA_CONFLICT);
    if (!created && b.isActive() != f.active()) access.requireAdmin();
    ContentManagementService.safeUrl(f.website());
    f.socialLinks().values().forEach(ContentManagementService::safeUrl);
    b.setName(f.name().strip());
    b.setIntroduction(f.introduction());
    b.setRepresentative(f.representative());
    b.setPhone(f.phone());
    b.setEmail(f.email());
    b.setWebsite(f.website());
    b.setSocialLinks(f.socialLinks());
    b.setActive(f.active());
    access.stamp(b, created);
    b.setLastManagementChange(java.time.Instant.now());
    businesses.saveAndFlush(b);
    changes.record("businesses", b, b.getId(), created ? "CREATE" : "UPDATE", "");
    return mapper.item(b);
  }

  public record Member(UUID membershipId, UUID userId, String displayName, String email) {}

  public List<Member> members(UUID id) {
    access.business(id);
    return members.findAll((r, q, b) -> b.equal(r.get("business").get("id"), id)).stream()
        .map(
            m ->
                new Member(
                    m.getId(),
                    m.getUser().getId(),
                    m.getUser().getDisplayName(),
                    m.getUser().getEmail()))
        .toList();
  }

  @Transactional
  public void assign(UUID businessId, UUID userId) {
    access.requireAdmin();
    Business business = access.business(businessId);
    User user =
        users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    if (user.getStatus() != User.Status.ACTIVE)
      throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
    if (members.count(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("business").get("id"), businessId),
                    b.equal(r.get("user").get("id"), userId)))
        > 0) return;
    if (user.getUserRoles().stream().noneMatch(r -> r.getRole().getCode().equals("PARTNER"))) {
      roles.ensureRole("PARTNER", "Đối tác", "Quản lý cơ sở và nội dung được phân công");
      user.addRole(roles.findByCode("PARTNER").orElseThrow(), access.current());
    }
    BusinessMember member = new BusinessMember();
    member.setBusiness(business);
    member.setUser(user);
    access.stamp(member, true);
    members.saveAndFlush(member);
    changes.record("businesses", business, businessId, "MEMBER_ASSIGNED", userId.toString());
  }

  @Transactional
  public void unassign(UUID businessId, UUID userId) {
    access.requireAdmin();
    Business business = access.business(businessId);
    members.deleteAll(
        members.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("business").get("id"), businessId),
                    b.equal(r.get("user").get("id"), userId))));
    changes.record("businesses", business, businessId, "MEMBER_REMOVED", userId.toString());
  }
}
