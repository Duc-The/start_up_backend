package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "import_batches")
public class ImportBatch extends BaseEntity {
  @Generated
  @ColumnDefault("'IMP-' || nextval('import_batches_import_id_seq')::text")
  @Column(
      name = "import_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String code;

  @Column(nullable = false)
  private String resource;

  @Column(nullable = false)
  private String status = "PREVIEW";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<Map<String, Object>> rows = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<UUID> importedIds = new ArrayList<>();

  @Column(nullable = false)
  private Instant expiresAt;

  @Version private long version;
}
