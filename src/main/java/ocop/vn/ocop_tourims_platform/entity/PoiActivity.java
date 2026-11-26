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
@Table(name = "poi_activities", uniqueConstraints =
        @UniqueConstraint(columnNames = {"poi_ref_id", "activity_ref_id"}))
public class PoiActivity extends BaseEntity {

    @Generated
    @ColumnDefault("'PAC-' || nextval('poi_activities_poi_activity_id_seq')::text")
    @Column(name = "poi_activity_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String poiActivityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poi poi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_ref_id", nullable = false, updatable = false)
    private Activity activity;

    protected PoiActivity() {
    }

    PoiActivity(Poi poi, Activity activity) {
        this.poi = Objects.requireNonNull(poi, "Địa điểm không được để trống");
        this.activity = Objects.requireNonNull(activity, "Thông tin liên kết không được để trống");
    }
}
