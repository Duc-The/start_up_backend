package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@BatchSize(size = 100)
@Table(
    name = "pois",
    check = {
      @CheckConstraint(constraint = "btrim(name) <> ''"),
      @CheckConstraint(constraint = "btrim(address) <> ''"),
      @CheckConstraint(
          constraint =
              "(rating_count = 0 AND rating_average = 0) "
                  + "OR (rating_count > 0 AND rating_average BETWEEN 1 AND 5)")
    })
public class Poi extends ManagedContent {

  @Generated
  @ColumnDefault("'POI-' || nextval('pois_poi_id_seq')::text")
  @Column(name = "poi_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String poiId;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "area_ref_id", nullable = false)
  private Area area;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Setter
  @Column(nullable = false, columnDefinition = "text")
  private String address;

  @Setter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "opening_hours", nullable = false, columnDefinition = "jsonb")
  @ColumnDefault("'{}'::jsonb")
  private Map<Day, List<OpeningInterval>> openingHours = new EnumMap<>(Day.class);

  @Setter
  @Column(name = "is_active", nullable = false)
  @ColumnDefault("true")
  private boolean active = true;

  @Setter
  @Column(name = "is_verified", nullable = false)
  @ColumnDefault("false")
  private boolean verified;

  @OneToOne(
      mappedBy = "poi",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private PoiGeometry geometry;

  @OneToMany(mappedBy = "manufacturerPoi")
  @BatchSize(size = 100)
  private Set<Product> manufacturedProducts = new HashSet<>();

  @OneToMany(mappedBy = "poi")
  @BatchSize(size = 100)
  private Set<ProductListing> productListings = new HashSet<>();

  @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
  @ColumnDefault("0")
  private BigDecimal ratingAverage = BigDecimal.ZERO;

  @Column(name = "rating_count", nullable = false)
  @ColumnDefault("0")
  private int ratingCount;

  public void updateRatingSummary(BigDecimal average, int count) {
    BigDecimal rounded = average.setScale(2, java.math.RoundingMode.HALF_UP);
    if (count < 0
        || (count == 0 && average.signum() != 0)
        || (count > 0
            && (average.compareTo(BigDecimal.ONE) < 0
                || average.compareTo(BigDecimal.valueOf(5)) > 0))) {
      throw new IllegalArgumentException("Thống kê đánh giá địa điểm không hợp lệ");
    }
    ratingAverage = rounded;
    ratingCount = count;
  }

  public void setCoordinates(double latitude, double longitude) {
    if (geometry == null) {
      geometry = new PoiGeometry(this, latitude, longitude);
    } else {
      geometry.setCoordinates(latitude, longitude);
    }
  }

  public void removeCoordinates() {
    geometry = null;
  }

  public enum Day {
    monday,
    tuesday,
    wednesday,
    thursday,
    friday,
    saturday,
    sunday
  }

  public record OpeningInterval(String open, String close) {}

  @OneToMany(mappedBy = "poi")
  @BatchSize(size = 100)
  private Set<Certification> certifications = new HashSet<>();

  @OneToMany(mappedBy = "poi")
  @BatchSize(size = 100)
  private Set<PoiReview> reviews = new HashSet<>();

  @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<PoiCategoryLink> categoryLinks = new HashSet<>();

  public Set<PoiCategoryLink> getCategoryLinks() {
    return Collections.unmodifiableSet(categoryLinks);
  }

  public PoiCategoryLink addCategory(PoiCategory category) {
    Objects.requireNonNull(category, "Thông tin liên kết không được để trống");
    boolean exists =
        categoryLinks.stream()
            .anyMatch(
                link ->
                    link.getCategory() == category
                        || (category.getId() != null
                            && category.getId().equals(link.getCategory().getId())));
    if (exists) {
      throw new IllegalArgumentException("Địa điểm đã có thông tin liên kết này");
    }
    PoiCategoryLink link = new PoiCategoryLink(this, category);
    categoryLinks.add(link);
    category.getPoiCategoryLinks().add(link);
    return link;
  }

  public void removeCategory(PoiCategoryLink link) {
    if (categoryLinks.remove(link)) {
      link.getCategory().getPoiCategoryLinks().remove(link);
    }
  }

  @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<PoiActivity> activities = new HashSet<>();

  public Set<PoiActivity> getActivities() {
    return Collections.unmodifiableSet(activities);
  }

  public PoiActivity addActivity(Activity activity) {
    Objects.requireNonNull(activity, "Thông tin liên kết không được để trống");
    boolean exists =
        activities.stream()
            .anyMatch(
                link ->
                    link.getActivity() == activity
                        || (activity.getId() != null
                            && activity.getId().equals(link.getActivity().getId())));
    if (exists) {
      throw new IllegalArgumentException("Địa điểm đã có thông tin liên kết này");
    }
    PoiActivity link = new PoiActivity(this, activity);
    activities.add(link);
    activity.getPoiActivities().add(link);
    return link;
  }

  public void removeActivity(PoiActivity link) {
    if (activities.remove(link)) {
      link.getActivity().getPoiActivities().remove(link);
    }
  }

  @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<PoiAmenity> amenities = new HashSet<>();

  public Set<PoiAmenity> getAmenities() {
    return Collections.unmodifiableSet(amenities);
  }

  public PoiAmenity addAmenity(Amenity amenity) {
    Objects.requireNonNull(amenity, "Thông tin liên kết không được để trống");
    boolean exists =
        amenities.stream()
            .anyMatch(
                link ->
                    link.getAmenity() == amenity
                        || (amenity.getId() != null
                            && amenity.getId().equals(link.getAmenity().getId())));
    if (exists) {
      throw new IllegalArgumentException("Địa điểm đã có thông tin liên kết này");
    }
    PoiAmenity link = new PoiAmenity(this, amenity);
    amenities.add(link);
    amenity.getPoiAmenities().add(link);
    return link;
  }

  public void removeAmenity(PoiAmenity link) {
    if (amenities.remove(link)) {
      link.getAmenity().getPoiAmenities().remove(link);
    }
  }

  @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("sortOrder ASC")
  private List<PoiMedia> media = new ArrayList<>();

  public List<PoiMedia> getMedia() {
    return Collections.unmodifiableList(media);
  }

  public PoiMedia addMedia(PoiMedia.Type type, String url, int sortOrder) {
    if (media.stream().anyMatch(item -> item.getSortOrder() == sortOrder)) {
      throw new IllegalArgumentException("Thứ tự tệp đã được sử dụng tại địa điểm này");
    }
    PoiMedia item = new PoiMedia(this, type, url, sortOrder);
    media.add(item);
    media.sort(Comparator.comparingInt(PoiMedia::getSortOrder));
    return item;
  }

  public void removeMedia(PoiMedia item) {
    media.remove(item);
  }

  @OneToMany(mappedBy = "poi")
  @BatchSize(size = 100)
  private Set<UserFavoritePoi> favorites = new HashSet<>();
}
