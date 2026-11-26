package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "product_listings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_ref_id", "poi_ref_id"})
}, check = {
    @CheckConstraint(constraint = "price_vnd > 0"),
    @CheckConstraint(constraint = "stock_quantity >= 0")
})
public class ProductListing extends BaseEntity {

    @Generated
    @ColumnDefault("'PLS-' || nextval('product_listings_product_listing_id_seq')::text")
    @Column(name = "product_listing_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String productListingId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_ref_id", nullable = false)
    private Product product;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false)
    private Poi poi;

    @Setter
    @Column(name = "price_vnd", nullable = false)
    private long priceVnd;

    @Setter
    @Column(name = "stock_quantity", nullable = false)
    @ColumnDefault("0")
    private int stockQuantity;

    @Setter
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean active = true;
}
