package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@BatchSize(size = 100)
@Table(name = "businesses")
public class Business extends BaseEntity {
  @Generated
  @ColumnDefault("'BUS-' || nextval('businesses_business_id_seq')::text")
  @Column(
      name = "business_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String introduction = "";

  @Column(nullable = false)
  private String representative = "";

  @Column(nullable = false)
  private String phone = "";

  @Column(nullable = false)
  private String email = "";

  @Column(nullable = false)
  private String website = "";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private java.util.Map<String, String> socialLinks = new java.util.HashMap<>();

  @Column(nullable = false)
  private boolean active = true;

  @Version private long version;
  private java.time.Instant lastManagementChange;
}
