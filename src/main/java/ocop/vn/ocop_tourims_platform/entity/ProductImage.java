package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
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
    name = "product_images",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"product_ref_id", "image_url"}),
      @UniqueConstraint(columnNames = {"product_ref_id", "sort_order"})
    },
    check = {
      @CheckConstraint(constraint = "btrim(image_url) <> ''"),
      @CheckConstraint(constraint = "sort_order >= 0")
    })
public class ProductImage extends BaseEntity {

  @Generated
  @ColumnDefault("'PIM-' || nextval('product_images_product_image_id_seq')::text")
  @Column(
      name = "product_image_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String productImageId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_ref_id", nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Product product;

  @Column(name = "image_url", nullable = false, columnDefinition = "text")
  private String imageUrl;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected ProductImage() {}

  ProductImage(Product product, String imageUrl, int sortOrder) {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("Đường dẫn ảnh không được trống");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("Thứ tự ảnh không được âm");
    }

    this.product = product;
    this.imageUrl = imageUrl;
    this.sortOrder = sortOrder;
  }
}
