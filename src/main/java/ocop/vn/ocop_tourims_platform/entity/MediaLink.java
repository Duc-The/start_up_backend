package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Getter
@Setter
@Entity
@BatchSize(size = 100)
@Table(
    name = "media_links",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"target_type", "target_id", "sort_order"}),
      @UniqueConstraint(columnNames = {"target_type", "target_id", "asset_id"})
    })
public class MediaLink extends BaseEntity {
  @Generated
  @ColumnDefault("'MLK-' || nextval('media_links_media_link_id_seq')::text")
  @Column(
      name = "media_link_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private MediaAsset asset;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private java.util.UUID targetId;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
