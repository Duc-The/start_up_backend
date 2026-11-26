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
@Table(name = "poi_category_links", uniqueConstraints =
        @UniqueConstraint(columnNames = {"poi_ref_id", "category_ref_id"}))
public class PoiCategoryLink extends BaseEntity {

    @Generated
    @ColumnDefault("'PCL-' || nextval('poi_category_links_poi_category_link_id_seq')::text")
    @Column(name = "poi_category_link_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String poiCategoryLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poi poi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_ref_id", nullable = false, updatable = false)
    private PoiCategory category;

    protected PoiCategoryLink() {
    }

    PoiCategoryLink(Poi poi, PoiCategory category) {
        this.poi = Objects.requireNonNull(poi, "Địa điểm không được để trống");
        this.category = Objects.requireNonNull(category, "Thông tin liên kết không được để trống");
    }
}
