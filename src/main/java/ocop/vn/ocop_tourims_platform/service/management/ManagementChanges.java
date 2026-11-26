package ocop.vn.ocop_tourims_platform.service.management;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.repository.management.ManagementAuditManagementRepository;
import ocop.vn.ocop_tourims_platform.service.HomeCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class ManagementChanges {
  public record Changed() {}

  private final ManagementAuditManagementRepository audit;
  private final ManagementAccess access;
  private final org.springframework.context.ApplicationEventPublisher events;
  private final HomeCache cache;

  public void record(String type, BaseEntity target, UUID business, String action, String detail) {
    access.stamp(target, false);
    ManagementAudit entry = new ManagementAudit();
    entry.setTargetType(type);
    entry.setTargetId(target.getId());
    entry.setBusinessId(business);
    entry.setAction(action);
    entry.setDetail(detail);
    access.stamp(entry, true);
    audit.save(entry);
    events.publishEvent(new Changed());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void changed(Changed event) {
    cache.invalidate();
  }
}
