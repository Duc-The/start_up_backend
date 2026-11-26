package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.Objects;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "poi_amenities", uniqueConstraints =
        @UniqueConstraint(columnNames = {"poi_ref_id", "amenity_ref_id"}))
public class PoiAmenity extends BaseEntity {

    @Generated
    @ColumnDefault("'PAM-' || nextval('poi_amenities_poi_amenity_id_seq')::text")
    @Column(name = "poi_amenity_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String poiAmenityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poi poi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_ref_id", nullable = false, updatable = false)
    private Amenity amenity;

    protected PoiAmenity() {
    }

    PoiAmenity(Poi poi, Amenity amenity) {
        this.poi = Objects.requireNonNull(poi, "Địa điểm không được để trống");
        this.amenity = Objects.requireNonNull(amenity, "Thông tin liên kết không được để trống");
    }
}
