package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Getter
@Setter
@Entity
@BatchSize(size = 100)
@Table(name = "management_audits")
public class ManagementAudit extends BaseEntity {
  @Generated
  @ColumnDefault("'AUD-' || nextval('management_audits_audit_id_seq')::text")
  @Column(name = "audit_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String code;

  @Column(nullable = false)
  private String targetType;

  @Column(nullable = false)
  private java.util.UUID targetId;

  private java.util.UUID businessId;

  @Column(nullable = false)
  private String action;

  @Column(nullable = false, columnDefinition = "text")
  private String detail = "";
}
