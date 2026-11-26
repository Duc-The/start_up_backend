package ocop.vn.ocop_tourims_platform;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ocop.vn.ocop_tourims_platform.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class HomeEntityTests {

    @PersistenceContext
    private EntityManager entities;

    @Test
    void persistsHomeRelationsAndRemovesOnlyPoiOwnedData() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Area area = new Area();
        area.setCode("AREA_" + suffix.toUpperCase());
        area.setName("Khu vực kiểm thử");
        entities.persist(area);

        PoiCategory category = new PoiCategory();
        category.setName("Điểm trải nghiệm");
        entities.persist(category);

        Activity activity = new Activity();
        activity.setCode("ACT_" + suffix.toUpperCase());
        activity.setName("Trải nghiệm");
        activity.setIconUrl("/icons/experience.svg");
        entities.persist(activity);

        Amenity amenity = new Amenity();
        amenity.setCode("AMN_" + suffix.toUpperCase());
        amenity.setName("Có bãi đỗ xe");
        amenity.setIconUrl("/icons/parking.svg");
        entities.persist(amenity);

        Poi poi = new Poi();
        poi.setArea(area);
        poi.setName("Cơ sở kiểm thử");
        poi.setAddress("Đà Nẵng");
        poi.setCoordinates(16.08, 108.15);
        PoiCategoryLink categoryLink = poi.addCategory(category);
        PoiActivity activityLink = poi.addActivity(activity);
        PoiAmenity amenityLink = poi.addAmenity(amenity);
        PoiMedia video = poi.addMedia(PoiMedia.Type.VIDEO, "/media/video.mp4", 1);
        poi.addMedia(PoiMedia.Type.IMAGE, "/media/cover.jpg", 0);
        assertThrows(IllegalArgumentException.class, () -> poi.addActivity(activity));
        assertThrows(IllegalArgumentException.class,
                () -> poi.addMedia(PoiMedia.Type.IMAGE, "/media/duplicate.jpg", 0));
        entities.persist(poi);

        User user = new User();
        user.setEmail("home-test-" + suffix + "@example.com");
        user.setDisplayName("Người dùng kiểm thử");
        entities.persist(user);
        UserFavoritePoi favorite = new UserFavoritePoi(user, poi);
        Notification notification = new Notification(user, "Địa điểm mới",
                "Khám phá địa điểm", "/pois/test");
        entities.persist(favorite);
        entities.persist(notification);
        entities.flush();

        assertNotNull(poi.getPoiId());
        assertNotNull(categoryLink.getPoiCategoryLinkId());
        assertNotNull(activityLink.getPoiActivityId());
        assertNotNull(amenityLink.getPoiAmenityId());
        assertNotNull(video.getPoiMediaId());
        assertNotNull(favorite.getFavoritePoiId());
        assertNotNull(notification.getNotificationId());
        entities.clear();

        Poi loaded = entities.find(Poi.class, poi.getId());
        assertEquals(1, loaded.getCategoryLinks().size());
        assertEquals(1, loaded.getActivities().size());
        assertEquals(1, loaded.getAmenities().size());
        assertEquals(PoiMedia.Type.IMAGE, loaded.getMedia().getFirst().getMediaType());
        loaded.removeActivity(loaded.getActivities().iterator().next());
        loaded.removeAmenity(loaded.getAmenities().iterator().next());
        loaded.removeCategory(loaded.getCategoryLinks().iterator().next());
        loaded.removeMedia(loaded.getMedia().getLast());
        entities.flush();
        entities.clear();

        assertNull(entities.find(PoiActivity.class, activityLink.getId()));
        assertNull(entities.find(PoiAmenity.class, amenityLink.getId()));
        assertNull(entities.find(PoiCategoryLink.class, categoryLink.getId()));
        assertNull(entities.find(PoiMedia.class, video.getId()));
        assertNotNull(entities.find(Activity.class, activity.getId()));
        assertNotNull(entities.find(Amenity.class, amenity.getId()));
        assertNotNull(entities.find(PoiCategory.class, category.getId()));

        Notification loadedNotification = entities.find(Notification.class, notification.getId());
        loadedNotification.markAsRead();
        entities.flush();
        entities.clear();
        assertTrue(entities.find(Notification.class, notification.getId()).isRead());

        entities.remove(entities.find(Poi.class, poi.getId()));
        entities.flush();
        entities.clear();
        assertNull(entities.find(UserFavoritePoi.class, favorite.getId()));
    }

    @Test
    void productTypesSupportHierarchyAndRejectCycles() {
        String suffix = UUID.randomUUID().toString();
        ProductType parent = productType("Thực phẩm", "food-" + suffix);
        ProductType child = productType("Nước mắm", "fish-sauce-" + suffix);
        child.setParent(parent);
        assertThrows(IllegalArgumentException.class, () -> parent.setParent(child));
        assertThrows(IllegalArgumentException.class, () -> child.setParent(child));
        entities.persist(parent);
        entities.persist(child);
        entities.flush();
        entities.clear();

        ProductType loadedParent = entities.find(ProductType.class, parent.getId());
        assertEquals(1, loadedParent.getChildren().size());
        assertEquals(parent.getId(),
                entities.find(ProductType.class, child.getId()).getParent().getId());
    }

    private ProductType productType(String name, String slug) {
        ProductType type = new ProductType();
        type.setName(name);
        type.setSlug(slug);
        type.setIconUrl("/icons/product.svg");
        return type;
    }
}
