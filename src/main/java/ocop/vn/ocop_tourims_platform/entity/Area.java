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
@Table(name = "areas", check = {
    @CheckConstraint(constraint = "code ~ '^[A-Z][A-Z0-9_]*$'"),
    @CheckConstraint(constraint = "btrim(name) <> ''")
})
public class Area extends BaseEntity {

    @Generated
    @ColumnDefault("'ARE-' || nextval('areas_area_id_seq')::text")
    @Column(name = "area_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String areaId;

    @Setter
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String code;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Setter
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @OneToMany(mappedBy = "area")
    @BatchSize(size = 100)
    private Set<Poi> pois = new HashSet<>();
}
