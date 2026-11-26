package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
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
    name = "poi_geometries",
    indexes = @Index(name = "idx_poi_geometries_location", columnList = "latitude,longitude"),
    check = {
      @CheckConstraint(constraint = "latitude BETWEEN -90 AND 90"),
      @CheckConstraint(constraint = "longitude BETWEEN -180 AND 180")
    })
public class PoiGeometry extends BaseEntity {

  @Generated
  @ColumnDefault("'POIG-' || nextval('poi_geometries_geometry_id_seq')::text")
  @Column(
      name = "geometry_id",
      nullable = false,
      unique = true,
      insertable = false,
      updatable = false)
  private String geometryId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "poi_ref_id", nullable = false, unique = true, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Poi poi;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  protected PoiGeometry() {}

  PoiGeometry(Poi poi, double latitude, double longitude) {
    this.poi = poi;
    setCoordinates(latitude, longitude);
  }

  void setCoordinates(double latitude, double longitude) {
    if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException("Vĩ độ phải từ -90 đến 90");
    }
    if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("Kinh độ phải từ -180 đến 180");
    }

    this.latitude = latitude;
    this.longitude = longitude;
  }
}
