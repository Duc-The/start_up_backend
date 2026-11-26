package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "amenities", check = {
    @CheckConstraint(constraint = "code ~ '^[A-Z][A-Z0-9_]*$'"),
    @CheckConstraint(constraint = "btrim(name) <> ''"),
    @CheckConstraint(constraint = "sort_order >= 0 AND btrim(icon_url) <> ''")
})
public class Amenity extends BaseEntity {

    @Generated
    @ColumnDefault("'AMN-' || nextval('amenities_amenity_id_seq')::text")
    @Column(name = "amenity_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String amenityId;

    @Setter
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String code;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Setter
    @Column(name = "icon_url", nullable = false, columnDefinition = "text")
    private String iconUrl;

    @Setter
    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    private int sortOrder;

    @Setter
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @OneToMany(mappedBy = "amenity")
    @BatchSize(size = 100)
    private Set<PoiAmenity> poiAmenities = new HashSet<>();
}
