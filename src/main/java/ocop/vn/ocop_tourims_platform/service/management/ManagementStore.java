package ocop.vn.ocop_tourims_platform.service.management;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.repository.management.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagementStore {
  private final PoiManagementRepository pois;
  private final ProductManagementRepository products;
  private final CertificationManagementRepository certificates;
  private final TourManagementRepository tours;
  private final StoryManagementRepository stories;
  private final TourismEventManagementRepository events;

  public ManagementRepository<? extends ManagedContent> repository(String resource) {
    return switch (resource) {
      case "pois" -> pois;
      case "products" -> products;
      case "certifications" -> certificates;
      case "tours" -> tours;
      case "stories" -> stories;
      case "events" -> events;
      default -> throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    };
  }

  public ManagedContent content(String type, UUID id) {
    return repository(type)
        .findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
