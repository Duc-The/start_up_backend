package ocop.vn.ocop_tourims_platform.service.management;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.*;
import ocop.vn.ocop_tourims_platform.dto.response.HomeResponse.PageData;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.mapper.ManagementMapper;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaManagementService {
  private final MediaAssetManagementRepository assets;
  private final MediaLinkManagementRepository links;
  private final BusinessManagementRepository businesses;
  private final ManagementAccess access;
  private final ManagementService management;
  private final ManagementStore store;
  private final ManagementMapper mapper;
  private final ManagementChanges changes;
  private final CloudinaryGateway cloud;

  public UploadTicket upload(Upload f) {
    var business = access.business(f.businessId());
    MediaAsset a = new MediaAsset();
    a.setBusiness(business);
    a.setResourceType(f.resourceType());
    a.setAltText(f.altText());
    a.setPublicId("ocop/" + business.getId() + "/" + UUID.randomUUID());
    long timestamp = Instant.now().getEpochSecond();
    a.setUploadExpiresAt(Instant.ofEpochSecond(timestamp).plusSeconds(900));
    var ticket = cloud.ticket(a.getPublicId(), a.getResourceType(), timestamp);
    access.stamp(a, true);
    assets.saveAndFlush(a);
    return new UploadTicket(
        a.getId(),
        ticket.url(),
        ticket.apiKey(),
        a.getPublicId(),
        timestamp,
        ticket.signature(),
        a.getUploadExpiresAt());
  }

  private MediaAsset owned(UUID id) {
    var a =
        assets.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    access.business(a.getBusiness().getId());
    return a;
  }

  public ManagementMapper.Media complete(UUID id) {
    MediaAsset a = owned(id);
    if (a.getStatus().equals("READY")) return mapper.media(a);
    if (!a.getStatus().equals("PENDING") || a.getUploadExpiresAt().isBefore(Instant.now()))
      ManagementAccess.invalid("Yêu cầu tải lên đã hết hạn hoặc đã bị xóa");
    var remote = cloud.inspect(a.getPublicId(), a.getResourceType());
    String type = String.valueOf(remote.get("resource_type")),
        format = String.valueOf(remote.get("format"));
    if (!a.getPublicId().equals(remote.get("public_id")) || !type.equals(a.getResourceType()))
      ManagementAccess.invalid("Tài nguyên Cloudinary không khớp yêu cầu");
    long bytes = number(remote, "bytes").longValue();
    Set<String> allowed =
        type.equals("image")
            ? Set.of("jpg", "jpeg", "png", "webp", "avif", "gif")
            : Set.of("mp4", "mov", "webm");
    if (!allowed.contains(format)
        || bytes <= 0
        || bytes > (type.equals("image") ? 10L : 200L) * 1024 * 1024)
      ManagementAccess.invalid("Định dạng hoặc dung lượng tệp không được hỗ trợ");
    String url = String.valueOf(remote.get("secure_url"));
    if (!url.startsWith("https://res.cloudinary.com/"))
      ManagementAccess.invalid("Đường dẫn Cloudinary không hợp lệ");
    a.setCloudinaryAssetId(String.valueOf(remote.get("asset_id")));
    a.setSecureUrl(url);
    a.setFormat(format);
    a.setBytes(bytes);
    a.setWidth(number(remote, "width").intValue());
    a.setHeight(number(remote, "height").intValue());
    if (remote.get("duration") instanceof Number duration) a.setDuration(duration.doubleValue());
    a.setOriginalFilename(
        String.valueOf(
            remote.containsKey("original_filename") ? remote.get("original_filename") : ""));
    a.setStatus("READY");
    access.stamp(a, false);
    assets.flush();
    changes.record("media", a, a.getBusiness().getId(), "UPLOAD", "");
    return mapper.media(a);
  }

  private Number number(Map<?, ?> remote, String key) {
    return remote.get(key) instanceof Number n ? n : 0;
  }

  @Transactional(readOnly = true)
  public PageData<ManagementMapper.Media> list(
      UUID businessId, String keyword, String resourceType, int page, int size) {
    boolean admin = access.admin();
    Set<UUID> owned = admin ? Set.of() : access.businessIds();
    if (businessId != null) access.business(businessId);
    var result =
        assets.findAll(
            (r, q, b) -> {
              var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
              p.add(b.notEqual(r.get("status"), "DELETED"));
              if (!admin) p.add(r.get("business").get("id").in(owned));
              if (businessId != null) p.add(b.equal(r.get("business").get("id"), businessId));
              if (keyword != null && !keyword.isBlank())
                p.add(
                    b.or(
                        b.like(
                            b.lower(r.get("originalFilename")),
                            ManagementService.pattern(keyword),
                            '!'),
                        b.like(
                            b.lower(r.get("altText")), ManagementService.pattern(keyword), '!')));
              if (resourceType != null) p.add(b.equal(r.get("resourceType"), resourceType));
              return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            ManagementService.pageable(page, size));
    return new PageData<>(
        result.getContent().stream().map(mapper::media).toList(),
        result.getTotalElements(),
        page,
        size);
  }

  public ManagementMapper.Media alt(UUID id, AltText f) {
    var a = owned(id);
    a.setAltText(f.altText());
    access.stamp(a, false);
    return mapper.media(a);
  }

  public void delete(UUID id) {
    var a = owned(id);
    if (a.getStatus().equals("DELETED")) return;
    if (links.count((r, q, b) -> b.equal(r.get("asset").get("id"), id)) > 0)
      throw new BusinessException(ErrorCode.DATA_CONFLICT);
    cloud.destroy(a.getPublicId(), a.getResourceType());
    a.setStatus("DELETED");
    access.stamp(a, false);
    changes.record("media", a, a.getBusiness().getId(), "DELETE", "");
  }

  public record Usage(String targetType, UUID targetId, int sortOrder) {}

  public List<Usage> usage(UUID id) {
    owned(id);
    return links.findAll((r, q, b) -> b.equal(r.get("asset").get("id"), id)).stream()
        .map(l -> new Usage(l.getTargetType(), l.getTargetId(), l.getSortOrder()))
        .toList();
  }

  public List<ManagementMapper.Media> attachments(String type, UUID id) {
    target(type, id);
    return links
        .findAll(
            (r, q, b) -> b.and(b.equal(r.get("targetType"), type), b.equal(r.get("targetId"), id)),
            org.springframework.data.domain.Sort.by("sortOrder"))
        .stream()
        .map(l -> mapper.media(l.getAsset()))
        .toList();
  }

  private BaseEntity target(String type, UUID id) {
    if (type.equals("businesses")) return access.business(id);
    ManagedContent c = store.content(type, id);
    access.content(c);
    return c;
  }

  public void attach(String type, UUID id, MediaAttachment f) {
    BaseEntity target = target(type, id);
    Business business = target instanceof Business b ? b : ((ManagedContent) target).getBusiness();
    if (business == null) ManagementAccess.invalid("Nội dung chưa được gán cơ sở");
    if (new HashSet<>(f.mediaIds()).size() != f.mediaIds().size())
      ManagementAccess.invalid("Danh sách ảnh/video bị trùng");
    if (target instanceof ManagedContent c) management.draft(c, false, f.version());
    if (target instanceof Business b && b.getVersion() != f.version())
      throw new BusinessException(ErrorCode.DATA_CONFLICT);
    if (target instanceof Business b) b.setLastManagementChange(Instant.now());
    // Lock assets in a stable order to serialize attachment and deletion without deadlocks.
    Map<UUID, MediaAsset> selected = new HashMap<>();
    if (!f.mediaIds().isEmpty())
      assets
          .lockAll(f.mediaIds())
          .forEach(
              a -> {
                if (!a.getBusiness().getId().equals(business.getId())
                    || !a.getStatus().equals("READY"))
                  ManagementAccess.invalid("Tài nguyên chưa sẵn sàng hoặc không thuộc cơ sở này");
                selected.put(a.getId(), a);
              });
    if (selected.size() != f.mediaIds().size())
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    if (type.equals("products")
        && selected.values().stream().anyMatch(a -> !a.getResourceType().equals("image")))
      ManagementAccess.invalid("Thư viện sản phẩm hiện hỗ trợ ảnh");
    var old =
        links.findAll(
            (r, q, b) -> b.and(b.equal(r.get("targetType"), type), b.equal(r.get("targetId"), id)));
    links.deleteAll(old);
    links.flush();
    if (target instanceof Product p) {
      new ArrayList<>(p.getImages()).forEach(p::removeImage);
      store.repository(type).flush();
    }
    if (target instanceof Poi p) {
      new ArrayList<>(p.getMedia()).forEach(p::removeMedia);
      store.repository(type).flush();
    }
    int order = 0;
    for (UUID mediaId : f.mediaIds()) {
      MediaAsset a = selected.get(mediaId);
      MediaLink l = new MediaLink();
      l.setAsset(a);
      l.setTargetType(type);
      l.setTargetId(id);
      l.setSortOrder(order);
      access.stamp(l, true);
      links.save(l);
      if (target instanceof Product p) p.addImage(a.getSecureUrl(), order);
      if (target instanceof Poi p)
        p.addMedia(
            a.getResourceType().equals("image") ? PoiMedia.Type.IMAGE : PoiMedia.Type.VIDEO,
            a.getSecureUrl(),
            order);
      order++;
    }
    access.stamp(target, false);
    changes.record(type, target, business.getId(), "MEDIA_UPDATE", "");
  }
}
