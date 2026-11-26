package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Getter
@Setter
@Entity
@BatchSize(size = 100)
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {
  @Generated
  @ColumnDefault("'MED-' || nextval('media_assets_media_id_seq')::text")
  @Column(name = "media_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Business business;

  @Column(nullable = false, unique = true)
  private String publicId;

  @Column(unique = true)
  private String cloudinaryAssetId;

  @Column(nullable = false)
  private String resourceType;

  @Column(nullable = false)
  private String status = "PENDING";

  @Column(nullable = false)
  private String secureUrl = "";

  @Column(nullable = false)
  private String originalFilename = "";

  @Column(nullable = false)
  private String format = "";

  @Column(nullable = false)
  private String altText = "";

  private long bytes;
  private int width;
  private int height;
  private Double duration;

  @Column(nullable = false)
  private java.time.Instant uploadExpiresAt;

  @Version private long version;
}
