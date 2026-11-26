package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "tour_stops",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tour_ref_id", "stop_order"})},
    check = {
      @CheckConstraint(constraint = "stop_order >= 1"),
      @CheckConstraint(constraint = "visit_duration_minutes >= 0")
    })
public class TourStop extends BaseEntity {

  @Generated
  @ColumnDefault("'TST-' || nextval('tour_stops_tour_stop_id_seq')::text")
  @Column(
      name = "tour_stop_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String tourStopId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tour_ref_id", nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Tour tour;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "poi_ref_id", nullable = false)
  private Poi poi;

  @Column(name = "stop_order", nullable = false)
  private int stopOrder;

  @Column(name = "visit_duration_minutes", nullable = false)
  private int visitDurationMinutes;

  protected TourStop() {}

  TourStop(Tour tour, Poi poi, int stopOrder, int visitDurationMinutes) {
    this.tour = tour;
    this.poi = Objects.requireNonNull(poi, "poi");
    this.stopOrder = stopOrder;
    setVisitDurationMinutes(visitDurationMinutes);
  }

  public void setVisitDurationMinutes(int minutes) {
    if (minutes < 0) {
      throw new IllegalArgumentException("Thời gian tham quan không được âm");
    }
    this.visitDurationMinutes = minutes;
  }
}
