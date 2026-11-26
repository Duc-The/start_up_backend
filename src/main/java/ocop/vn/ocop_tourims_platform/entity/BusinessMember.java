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
    name = "business_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "user_id"}))
public class BusinessMember extends BaseEntity {
  @Generated
  @ColumnDefault("'BM-' || nextval('business_members_business_member_id_seq')::text")
  @Column(
      name = "business_member_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Business business;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private User user;
}
