package ocop.vn.ocop_tourims_platform.service.management;

import com.cloudinary.Cloudinary;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CloudinaryGateway {
  private final Cloudinary cloudinary;

  public record Ticket(String url, String apiKey, String signature) {}

  public Ticket ticket(String publicId, String type, long timestamp) {
    if (cloudinary.config.cloudName == null
        || cloudinary.config.cloudName.isBlank()
        || cloudinary.config.apiSecret == null
        || cloudinary.config.apiSecret.isBlank()
        || cloudinary.config.apiKey == null
        || cloudinary.config.apiKey.isBlank())
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
    var params = new HashMap<String, Object>();
    params.put("public_id", publicId);
    params.put("timestamp", timestamp);
    params.put("overwrite", false);
    String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret, 1);
    return new Ticket(
        "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/" + type + "/upload",
        cloudinary.config.apiKey,
        signature);
  }

  public Map<?, ?> inspect(String publicId, String type) {
    try {
      return cloudinary.api().resource(publicId, Map.of("resource_type", type, "type", "upload"));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
    }
  }

  public void destroy(String publicId, String type) {
    try {
      var result =
          cloudinary
              .uploader()
              .destroy(publicId, Map.of("resource_type", type, "invalidate", true));
      if (!Set.of("ok", "not found").contains(result.get("result")))
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
    }
  }
}
