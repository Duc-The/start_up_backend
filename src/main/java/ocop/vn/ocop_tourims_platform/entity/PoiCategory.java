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
@Table(name = "poi_categories", check = {
    @CheckConstraint(constraint = "btrim(name) <> ''"),
    @CheckConstraint(constraint = "color_hex ~ '^#[0-9A-Fa-f]{6}$'"),
    @CheckConstraint(constraint = "parent_ref_id <> id")
})
public class PoiCategory extends BaseEntity {

    @Generated
    @ColumnDefault("'POIC-' || nextval('poi_categories_category_id_seq')::text")
    @Column(name = "category_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String categoryId;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Setter
    @Column(name = "icon_url", columnDefinition = "text")
    private String iconUrl;

    @Setter
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ref_id")
    private PoiCategory parent;

    @OneToMany(mappedBy = "parent")
    @BatchSize(size = 100)
    private Set<PoiCategory> children = new HashSet<>();

    @OneToMany(mappedBy = "category")
    @BatchSize(size = 100)
    private Set<PoiCategoryLink> poiCategoryLinks = new HashSet<>();
}
