package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "order_items",
    check = {
      @CheckConstraint(constraint = "quantity > 0 AND unit_price_vnd > 0"),
      @CheckConstraint(constraint = "btrim(product_name) <> ''")
    })
public class OrderItem extends BaseEntity {
  public enum FulfillmentStatus {
    PENDING,
    SHIPPED,
    COMPLETED
  }

  @lombok.Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PENDING'")
  private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.PENDING;

  @Generated
  @ColumnDefault("'ORI-' || nextval('order_items_order_item_id_seq')::text")
  @Column(
      name = "order_item_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String orderItemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_ref_id", nullable = false, updatable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_listing_ref_id", nullable = false, updatable = false)
  private ProductListing productListing;

  @Column(name = "product_name", nullable = false, updatable = false, columnDefinition = "text")
  private String productName;

  @Column(nullable = false, updatable = false)
  private int quantity;

  @Column(name = "unit_price_vnd", nullable = false, updatable = false)
  private long unitPriceVnd;

  protected OrderItem() {}

  OrderItem(Order order, ProductListing listing, int quantity) {
    this.order = order;
    this.productListing = Objects.requireNonNull(listing, "listing");
    if (quantity <= 0 || listing.getPriceVnd() <= 0) {
      throw new IllegalArgumentException("Số lượng và đơn giá phải lớn hơn 0");
    }
    this.productName = listing.getProduct().getName();
    this.quantity = quantity;
    this.unitPriceVnd = listing.getPriceVnd();
  }

  public long getLineTotalVnd() {
    return Math.multiplyExact(unitPriceVnd, quantity);
  }
}
