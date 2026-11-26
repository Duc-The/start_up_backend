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
@Table(name = "user_favorite_pois", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_ref_id", "poi_ref_id"}))
public class UserFavoritePoi extends BaseEntity {

    @Generated
    @ColumnDefault("'FPO-' || nextval('user_favorite_pois_favorite_poi_id_seq')::text")
    @Column(name = "favorite_poi_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String favoritePoiId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poi poi;

    protected UserFavoritePoi() {
    }

    public UserFavoritePoi(User user, Poi poi) {
        this.user = Objects.requireNonNull(user, "Người dùng không được để trống");
        this.poi = Objects.requireNonNull(poi, "Địa điểm không được để trống");
    }
}
