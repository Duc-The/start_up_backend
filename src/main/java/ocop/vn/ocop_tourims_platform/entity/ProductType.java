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
@Table(name = "product_types", check = {
    @CheckConstraint(constraint = "btrim(name) <> ''"),
    @CheckConstraint(constraint = "parent_ref_id <> id"),
    @CheckConstraint(constraint = "sort_order >= 0 AND btrim(icon_url) <> ''"),
    @CheckConstraint(constraint = "slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'")
})
public class ProductType extends BaseEntity {

    @Generated
    @ColumnDefault("'PTY-' || nextval('product_types_product_type_id_seq')::text")
    @Column(name = "product_type_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String productTypeId;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Setter
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String slug;

    @Setter
    @Column(name = "icon_url", nullable = false, columnDefinition = "text")
    private String iconUrl;

    @Setter
    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ref_id")
    private ProductType parent;

    @OneToMany(mappedBy = "parent")
    @BatchSize(size = 100)
    private Set<ProductType> children = new HashSet<>();

    public void setParent(ProductType parent) {
        for (ProductType ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor == this || (getId() != null && getId().equals(ancestor.getId()))) {
                throw new IllegalArgumentException("Danh mục cha không được tạo thành vòng lặp");
            }
        }
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    @Setter
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @OneToMany(mappedBy = "productType")
    @BatchSize(size = 100)
    private Set<Product> products = new HashSet<>();
}
