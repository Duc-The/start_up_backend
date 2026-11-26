package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "products",
    check = {
      @CheckConstraint(constraint = "btrim(name) <> ''"),
      @CheckConstraint(constraint = "btrim(description) <> ''")
    })
public class Product extends ManagedContent {

  @Generated
  @ColumnDefault("'PRD-' || nextval('products_product_id_seq')::text")
  @Column(
      name = "product_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String productId;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_type_ref_id", nullable = false)
  private ProductType productType;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "manufacturer_poi_ref_id", nullable = false)
  private Poi manufacturerPoi;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Setter
  @Column(name = "is_active", nullable = false)
  @ColumnDefault("true")
  private boolean active = true;

  @OneToMany(mappedBy = "product")
  @BatchSize(size = 100)
  private Set<ProductListing> listings = new HashSet<>();

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("sortOrder ASC")
  private List<ProductImage> images = new ArrayList<>();

  public List<ProductImage> getImages() {
    return Collections.unmodifiableList(images);
  }

  public ProductImage addImage(String imageUrl, int sortOrder) {
    if (images.stream()
        .anyMatch(
            image -> image.getSortOrder() == sortOrder || image.getImageUrl().equals(imageUrl))) {
      throw new IllegalArgumentException("Đường dẫn và thứ tự ảnh của sản phẩm không được trùng");
    }

    ProductImage image = new ProductImage(this, imageUrl, sortOrder);
    images.add(image);
    images.sort(Comparator.comparingInt(ProductImage::getSortOrder));
    return image;
  }

  public void removeImage(ProductImage image) {
    images.remove(image);
  }

  @OneToMany(mappedBy = "product")
  @BatchSize(size = 100)
  private Set<Certification> certifications = new HashSet<>();

  @OneToMany(mappedBy = "product")
  @BatchSize(size = 100)
  private Set<ProductReview> reviews = new HashSet<>();
}
