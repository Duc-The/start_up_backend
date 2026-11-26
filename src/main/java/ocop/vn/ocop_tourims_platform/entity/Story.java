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
@Table(name = "stories")
public class Story extends ManagedContent {
  @Generated
  @ColumnDefault("'STO-' || nextval('stories_story_id_seq')::text")
  @Column(name = "story_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Poi poi;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false, columnDefinition = "text")
  private String introduction;

  @Column(nullable = false)
  private String founder = "";

  private Integer foundedYear;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private java.util.List<Section> sections = new java.util.ArrayList<>();

  public record Section(String heading, String body, java.util.List<java.util.UUID> mediaIds) {}
}
