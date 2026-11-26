package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "tour_route_segments",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"from_stop_ref_id", "to_stop_ref_id"})},
    check = {
      @CheckConstraint(constraint = "from_stop_ref_id <> to_stop_ref_id"),
      @CheckConstraint(constraint = "distance_meters >= 0 AND duration_seconds >= 0"),
      @CheckConstraint(
          constraint =
              "jsonb_typeof(path_coordinates) = 'array' AND jsonb_array_length(path_coordinates) >="
                  + " 2")
    })
public class TourRouteSegment extends BaseEntity {

  @Generated
  @ColumnDefault("'TRS-' || nextval('tour_route_segments_tour_route_segment_id_seq')::text")
  @Column(
      name = "tour_route_segment_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String tourRouteSegmentId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_stop_ref_id", nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private TourStop fromStop;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_stop_ref_id", nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private TourStop toStop;

  @Enumerated(EnumType.STRING)
  @Column(name = "travel_mode", nullable = false)
  private TravelMode travelMode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "path_coordinates", nullable = false, columnDefinition = "jsonb")
  private List<Coordinate> pathCoordinates;

  @Column(name = "distance_meters", nullable = false)
  private int distanceMeters;

  @Column(name = "duration_seconds", nullable = false)
  private int durationSeconds;

  protected TourRouteSegment() {}

  public TourRouteSegment(
      TourStop fromStop,
      TourStop toStop,
      TravelMode travelMode,
      List<Coordinate> pathCoordinates,
      int distanceMeters,
      int durationSeconds) {
    Tour fromTour = fromStop.getTour();
    Tour toTour = toStop.getTour();
    boolean sameTour =
        fromTour == toTour || (fromTour.getId() != null && fromTour.getId().equals(toTour.getId()));
    if (!sameTour || fromStop.getStopOrder() + 1 != toStop.getStopOrder()) {
      throw new IllegalArgumentException("Điểm dừng phải liên tiếp và thuộc cùng một tour");
    }

    this.fromStop = fromStop;
    this.toStop = toStop;
    updateRoute(travelMode, pathCoordinates, distanceMeters, durationSeconds);
  }

  public List<Coordinate> getPathCoordinates() {
    return List.copyOf(pathCoordinates);
  }

  public void updateRoute(
      TravelMode travelMode,
      List<Coordinate> pathCoordinates,
      int distanceMeters,
      int durationSeconds) {
    Objects.requireNonNull(travelMode, "travelMode");
    List<Coordinate> coordinates = List.copyOf(pathCoordinates);
    if (coordinates.size() < 2 || distanceMeters < 0 || durationSeconds < 0) {
      throw new IllegalArgumentException(
          "Tuyến đường cần ít nhất hai tọa độ, khoảng cách và thời gian không được âm");
    }

    this.travelMode = travelMode;
    this.pathCoordinates = coordinates;
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
  }

  public enum TravelMode {
    WALKING,
    DRIVING
  }

  public record Coordinate(double lat, double lng) {
    public Coordinate {
      if (!Double.isFinite(lat)
          || lat < -90
          || lat > 90
          || !Double.isFinite(lng)
          || lng < -180
          || lng > 180) {
        throw new IllegalArgumentException("Tọa độ tuyến đường không hợp lệ");
      }
    }
  }
}
