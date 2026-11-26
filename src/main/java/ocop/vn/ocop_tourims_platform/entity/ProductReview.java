package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "product_reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_ref_id", "product_ref_id"}),
    check = @CheckConstraint(constraint = "rating BETWEEN 1 AND 5"))
public class ProductReview extends BaseEntity {

  @Generated
  @ColumnDefault("'PDR-' || nextval('product_reviews_product_review_id_seq')::text")
  @Column(
      name = "product_review_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String productReviewId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_ref_id", nullable = false, updatable = false)
  private Product product;

  @Column(nullable = false)
  private short rating;

  @Column(nullable = false, columnDefinition = "text")
  @ColumnDefault("''")
  private String comment = "";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PUBLISHED'")
  private ReviewStatus status = ReviewStatus.PUBLISHED;

  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("sortOrder ASC")
  private List<ProductReviewMedia> media = new ArrayList<>();

  protected ProductReview() {}

  public ProductReview(User author, Product product, int rating, String comment) {
    this.author = Objects.requireNonNull(author, "author");
    this.product = Objects.requireNonNull(product, "product");
    updateReview(rating, comment);
  }

  public void updateReview(int rating, String comment) {
    if (rating < 1 || rating > 5) {
      throw new IllegalArgumentException("Số sao đánh giá phải từ 1 đến 5");
    }
    this.rating = (short) rating;
    this.comment = comment == null ? "" : comment;
  }

  public void setStatus(ReviewStatus status) {
    this.status = Objects.requireNonNull(status, "status");
  }

  public List<ProductReviewMedia> getMedia() {
    return Collections.unmodifiableList(media);
  }

  public ProductReviewMedia addMedia(ReviewMediaType type, String url, int sortOrder) {
    if (media.stream().anyMatch(item -> item.getSortOrder() == sortOrder)) {
      throw new IllegalArgumentException("Thứ tự ảnh/video trong đánh giá không được trùng");
    }
    ProductReviewMedia attachment = new ProductReviewMedia(this, type, url, sortOrder);
    media.add(attachment);
    media.sort(Comparator.comparingInt(ProductReviewMedia::getSortOrder));
    return attachment;
  }

  public void removeMedia(ProductReviewMedia attachment) {
    media.remove(attachment);
  }
}
