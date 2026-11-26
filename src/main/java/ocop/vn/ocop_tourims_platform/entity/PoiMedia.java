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
@Table(name = "poi_media", uniqueConstraints =
        @UniqueConstraint(columnNames = {"poi_ref_id", "sort_order"}),
        check = @CheckConstraint(constraint = "sort_order >= 0 AND btrim(media_url) <> ''"))
public class PoiMedia extends BaseEntity {

    @Generated
    @ColumnDefault("'PMD-' || nextval('poi_media_poi_media_id_seq')::text")
    @Column(name = "poi_media_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String poiMediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Poi poi;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private Type mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "text")
    private String mediaUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PoiMedia() {
    }

    PoiMedia(Poi poi, Type type, String url, int sortOrder) {
        if (url == null || url.isBlank() || sortOrder < 0) {
            throw new IllegalArgumentException("Đường dẫn tệp phải có giá trị và thứ tự phải từ 0 trở lên");
        }
        this.poi = Objects.requireNonNull(poi, "Địa điểm không được để trống");
        this.mediaType = Objects.requireNonNull(type, "Loại tệp không được để trống");
        this.mediaUrl = url;
        this.sortOrder = sortOrder;
    }

    public enum Type {
        IMAGE, VIDEO
    }
}
