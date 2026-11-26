package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "product_review_media",
    uniqueConstraints = @UniqueConstraint(columnNames = {"review_ref_id", "sort_order"}),
    check = @CheckConstraint(constraint = "sort_order >= 0 AND btrim(media_url) <> ''"))
public class ProductReviewMedia extends BaseEntity {

  @Generated
  @ColumnDefault("'PDRM-' || nextval('product_review_media_product_review_media_id_seq')::text")
  @Column(
      name = "product_review_media_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String productReviewMediaId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "review_ref_id", nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ProductReview review;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false)
  private ReviewMediaType mediaType;

  @Column(name = "media_url", nullable = false, columnDefinition = "text")
  private String mediaUrl;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected ProductReviewMedia() {}

  ProductReviewMedia(ProductReview review, ReviewMediaType type, String url, int sortOrder) {
    if (url == null || url.isBlank() || sortOrder < 0) {
      throw new IllegalArgumentException("Ảnh/video cần đường dẫn và thứ tự từ 0 trở lên");
    }
    this.review = review;
    this.mediaType = Objects.requireNonNull(type, "type");
    this.mediaUrl = url;
    this.sortOrder = sortOrder;
  }
}
