package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "tours",
    check = {
      @CheckConstraint(constraint = "slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'"),
      @CheckConstraint(constraint = "btrim(name) <> ''"),
      @CheckConstraint(constraint = "btrim(description) <> ''")
    })
public class Tour extends ManagedContent {

  @Generated
  @ColumnDefault("'TOU-' || nextval('tours_tour_id_seq')::text")
  @Column(name = "tour_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String tourId;

  @Setter
  @Column(nullable = false, unique = true, columnDefinition = "text")
  private String slug;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'DRAFT'")
  private Status status = Status.DRAFT;

  @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("stopOrder ASC")
  private List<TourStop> stops = new ArrayList<>();

  public List<TourStop> getStops() {
    return Collections.unmodifiableList(stops);
  }

  public TourStop addStop(Poi poi, int visitDurationMinutes) {
    int nextOrder = stops.isEmpty() ? 1 : stops.getLast().getStopOrder() + 1;
    TourStop stop = new TourStop(this, poi, nextOrder, visitDurationMinutes);
    stops.add(stop);
    return stop;
  }

  public void clearStops() {
    stops.clear();
  }

  public enum Status {
    DRAFT,
    PUBLISHED,
    ARCHIVED
  }
}
