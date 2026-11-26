package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "certifications",
    check = {
      @CheckConstraint(constraint = "(product_ref_id IS NOT NULL) <> (poi_ref_id IS NOT NULL)"),
      @CheckConstraint(
          constraint =
              "(certificate_type = 'OCOP' AND product_ref_id IS NOT NULL "
                  + "AND ocop_stars IS NOT NULL AND ocop_stars BETWEEN 3 AND 5) "
                  + "OR (certificate_type = 'OTHER' AND ocop_stars IS NULL)"),
      @CheckConstraint(constraint = "expires_on >= issued_on")
    })
public class Certification extends ManagedContent {

  @Generated
  @ColumnDefault("'CRT-' || nextval('certifications_certification_id_seq')::text")
  @Column(
      name = "certification_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String certificationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_ref_id", updatable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poi_ref_id", updatable = false)
  private Poi poi;

  @Enumerated(EnumType.STRING)
  @Column(name = "certificate_type", nullable = false, updatable = false)
  private Type type;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Setter
  @Column(name = "certificate_number", nullable = false, columnDefinition = "text")
  private String certificateNumber;

  @Setter
  @Column(name = "issuing_authority", nullable = false, columnDefinition = "text")
  private String issuingAuthority;

  @Setter
  @Column(name = "issued_on", nullable = false)
  private LocalDate issuedOn;

  @Setter
  @Column(name = "expires_on")
  private LocalDate expiresOn;

  @Column(name = "ocop_stars", updatable = false)
  private Short ocopStars;

  protected Certification() {}

  public Certification(Product product, Type type, Short ocopStars) {
    this.product = Objects.requireNonNull(product, "product");
    this.type = Objects.requireNonNull(type, "type");
    if (type == Type.OCOP && (ocopStars == null || ocopStars < 3 || ocopStars > 5)) {
      throw new IllegalArgumentException("Chứng nhận OCOP phải có từ 3 đến 5 sao");
    }
    if (type == Type.OTHER && ocopStars != null) {
      throw new IllegalArgumentException("Chỉ chứng nhận OCOP mới có số sao");
    }
    this.ocopStars = ocopStars;
  }

  public Certification(Poi poi) {
    this.poi = Objects.requireNonNull(poi, "poi");
    this.type = Type.OTHER;
  }

  public enum Type {
    OCOP,
    OTHER
  }
}
