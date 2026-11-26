package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@MappedSuperclass
public abstract class ManagedContent extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_ref_id")
  private Business business;

  @Enumerated(EnumType.STRING)
  @Column(name = "publication_status", nullable = false)
  @ColumnDefault("'PUBLISHED'")
  private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;

  @Column(name = "rejection_reason", nullable = false, columnDefinition = "text")
  @ColumnDefault("''")
  private String rejectionReason = "";

  @Version
  @Column(nullable = false)
  @ColumnDefault("0")
  private long version;

  private java.time.Instant lastManagementChange;
}
