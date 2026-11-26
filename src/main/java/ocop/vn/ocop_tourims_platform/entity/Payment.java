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
    name = "payments",
    check = {
      @CheckConstraint(constraint = "amount_vnd > 0 AND payos_order_code > 0"),
      @CheckConstraint(
          constraint = "payos_payment_link_id IS NULL OR btrim(payos_payment_link_id) <> ''")
    })
public class Payment extends BaseEntity {

  @Generated
  @ColumnDefault("'PAY-' || nextval('payments_payment_id_seq')::text")
  @Column(
      name = "payment_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String paymentId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_ref_id", nullable = false, updatable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  @ColumnDefault("'PAYOS_VIETQR'")
  private Method method = Method.PAYOS_VIETQR;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'PENDING'")
  private Status status = Status.PENDING;

  @Column(name = "amount_vnd", nullable = false, updatable = false)
  private long amountVnd;

  @Generated
  @ColumnDefault("nextval('payments_payos_order_code_seq')")
  @Column(
      name = "payos_order_code",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private Long payosOrderCode;

  @Column(name = "payos_payment_link_id", unique = true, columnDefinition = "text")
  private String payosPaymentLinkId;

  protected Payment() {}

  public Payment(Order order) {
    this.order = Objects.requireNonNull(order, "order");
    if (order.getStatus() != Order.Status.PENDING_PAYMENT
        || order.getItems().isEmpty()
        || order.getTotalVnd() <= 0) {
      throw new IllegalArgumentException("Đơn hàng chưa đáp ứng điều kiện thanh toán");
    }
    this.amountVnd = order.getTotalVnd();
  }

  public void recordPayosLink(String paymentLinkId) {
    if (paymentLinkId == null || paymentLinkId.isBlank()) {
      throw new IllegalArgumentException("Thiếu mã liên kết thanh toán payOS");
    }
    if (payosPaymentLinkId != null && !payosPaymentLinkId.equals(paymentLinkId)) {
      throw new IllegalStateException("A different payOS link is already recorded");
    }
    payosPaymentLinkId = paymentLinkId;
  }

  public enum Method {
    PAYOS_VIETQR
  }

  public enum Status {
    PENDING,
    PAID,
    CANCELLED,
    EXPIRED,
    FAILED
  }
}
