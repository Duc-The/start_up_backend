package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity(name = "CustomerOrder")
@BatchSize(size = 100)
@Table(
    name = "orders",
    check = {
      @CheckConstraint(
          constraint =
              "subtotal_vnd >= 0 AND shipping_fee_vnd >= 0 AND total_vnd = subtotal_vnd +"
                  + " shipping_fee_vnd"),
      @CheckConstraint(
          constraint =
              "btrim(recipient_name) <> '' AND btrim(recipient_phone) <> '' AND"
                  + " btrim(delivery_address) <> ''")
    })
public class Order extends BaseEntity {

  @Generated
  @ColumnDefault("'ORD-' || nextval('orders_order_id_seq')::text")
  @Column(name = "order_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String orderId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
  private User buyer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PENDING_PAYMENT'")
  private Status status = Status.PENDING_PAYMENT;

  @Column(name = "recipient_name", nullable = false, columnDefinition = "text")
  private String recipientName;

  @Column(name = "recipient_phone", nullable = false, columnDefinition = "text")
  private String recipientPhone;

  @Column(name = "delivery_address", nullable = false, columnDefinition = "text")
  private String deliveryAddress;

  @Column(name = "subtotal_vnd", nullable = false)
  private long subtotalVnd;

  @Column(name = "shipping_fee_vnd", nullable = false)
  private long shippingFeeVnd;

  @Column(name = "total_vnd", nullable = false)
  private long totalVnd;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private List<OrderItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "order")
  @BatchSize(size = 100)
  private List<Payment> payments = new ArrayList<>();

  protected Order() {}

  public Order(
      User buyer,
      String recipientName,
      String recipientPhone,
      String deliveryAddress,
      long shippingFeeVnd) {
    this.buyer = Objects.requireNonNull(buyer, "buyer");
    if (recipientName == null
        || recipientName.isBlank()
        || recipientPhone == null
        || recipientPhone.isBlank()
        || deliveryAddress == null
        || deliveryAddress.isBlank()
        || shippingFeeVnd < 0) {
      throw new IllegalArgumentException("Thông tin giao hàng hoặc phí vận chuyển không hợp lệ");
    }
    this.recipientName = recipientName;
    this.recipientPhone = recipientPhone;
    this.deliveryAddress = deliveryAddress;
    this.shippingFeeVnd = shippingFeeVnd;
    this.totalVnd = shippingFeeVnd;
  }

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public List<Payment> getPayments() {
    return Collections.unmodifiableList(payments);
  }

  public OrderItem addItem(ProductListing listing, int quantity) {
    if (getId() != null) {
      throw new IllegalStateException("Order items cannot change after checkout");
    }
    OrderItem item = new OrderItem(this, listing, quantity);
    long subtotal = Math.addExact(subtotalVnd, item.getLineTotalVnd());
    long total = Math.addExact(subtotal, shippingFeeVnd);
    items.add(item);
    subtotalVnd = subtotal;
    totalVnd = total;
    return item;
  }

  public enum Status {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED
  }

  public void updateFulfillmentStatus(Status next) {
    if (!((status == Status.PAID && next == Status.SHIPPED)
        || (status == Status.SHIPPED && next == Status.COMPLETED))) {
      throw new IllegalArgumentException("Không thể chuyển trạng thái đơn hàng theo yêu cầu");
    }
    status = next;
  }
}
