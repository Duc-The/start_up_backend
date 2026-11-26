package ocop.vn.ocop_tourims_platform.dto.response;

import java.time.Instant;
import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.PublicationStatus;

public final class ManagementResponse {
  private ManagementResponse() {}

  public record Item(
      UUID id,
      String code,
      String name,
      UUID businessId,
      PublicationStatus publicationStatus,
      String rejectionReason,
      Long version,
      Object details) {}

  public record UploadTicket(
      UUID id,
      String uploadUrl,
      String apiKey,
      String publicId,
      long timestamp,
      String signature,
      Instant expiresAt) {}

  public record Dashboard(Map<String, Long> counts) {}
}
