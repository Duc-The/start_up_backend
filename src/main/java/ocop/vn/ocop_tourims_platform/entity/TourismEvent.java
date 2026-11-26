package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

@Getter
@Setter
@Entity
@BatchSize(size = 100)
@Table(name = "tourism_events")
public class TourismEvent extends ManagedContent {
  @Generated
  @ColumnDefault("'EVT-' || nextval('tourism_events_event_id_seq')::text")
  @Column(name = "event_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Poi poi;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private java.time.Instant startsAt;

  @Column(nullable = false)
  private java.time.Instant endsAt;

  @Column(nullable = false)
  private String organizer;

  @Column(nullable = false)
  private String registrationUrl = "";

  @Column(nullable = false)
  private String recurrence = "";
}
