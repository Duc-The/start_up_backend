package ocop.vn.ocop_tourims_platform.service.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.dto.response.ManagementResponse.UploadTicket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogImageService {
  private final ManagementAccess access;
  private final CloudinaryGateway cloud;

  public record Complete(
      @NotBlank(message = "Thiếu mã ảnh đã tải lên")
          @Pattern(
              regexp =
                  "ocop/catalog-icons/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
              message = "Mã ảnh danh mục không hợp lệ")
          String publicId) {}

  public record Image(UUID id, String secureUrl) {}

  public UploadTicket upload() {
    access.requireAdmin();
    UUID id = UUID.randomUUID();
    String publicId = "ocop/catalog-icons/" + id;
    long timestamp = Instant.now().getEpochSecond();
    var ticket = cloud.ticket(publicId, "image", timestamp);
    return new UploadTicket(
        id,
        ticket.url(),
        ticket.apiKey(),
        publicId,
        timestamp,
        ticket.signature(),
        Instant.ofEpochSecond(timestamp).plusSeconds(900));
  }

  public Image complete(Complete request) {
    access.requireAdmin();
    var remote = cloud.inspect(request.publicId(), "image");
    if (!request.publicId().equals(remote.get("public_id"))
        || !"image".equals(remote.get("resource_type"))
        || !Set.of("jpg", "jpeg", "png", "webp", "avif", "gif").contains(remote.get("format"))
        || !(remote.get("bytes") instanceof Number bytes)
        || bytes.longValue() <= 0
        || bytes.longValue() > 10L * 1024 * 1024)
      ManagementAccess.invalid(
          "Ảnh không hợp lệ. Chỉ hỗ trợ JPG, PNG, WEBP, AVIF, GIF, tối đa 10 MB");
    String url = String.valueOf(remote.get("secure_url"));
    if (!url.startsWith("https://res.cloudinary.com/"))
      ManagementAccess.invalid("Đường dẫn ảnh Cloudinary không hợp lệ");
    return new Image(
        UUID.fromString(request.publicId().substring("ocop/catalog-icons/".length())), url);
  }
}
