package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.*;
import java.util.*;
import ocop.vn.ocop_tourims_platform.dto.request.PoiFilter;
import ocop.vn.ocop_tourims_platform.entity.*;
import ocop.vn.ocop_tourims_platform.service.HomeService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class PoiSpecificationTests {
  @PersistenceContext EntityManager em;
  @Autowired HomeService home;
  @Autowired EntityManagerFactory factory;

  @Test
  void paginationAndCardLoadingUseBoundedQueries() {
    Area area = new Area();
    area.setCode("SPEC_" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
    area.setName("Khu vực kiểm tra Specification");
    em.persist(area);
    PoiCategory category = new PoiCategory();
    category.setName("Danh mục thử");
    em.persist(category);
    for (int i = 0; i < 12; i++) {
      Poi poi = new Poi();
      poi.setName(String.format("Địa điểm %02d", i));
      poi.setAddress("Đà Nẵng");
      poi.setArea(area);
      poi.setCoordinates(16 + i * 0.001, 108);
      poi.addCategory(category);
      poi.addMedia(PoiMedia.Type.IMAGE, "/media/" + i, 0);
      em.persist(poi);
    }
    em.flush();
    em.clear();
    var statistics = factory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    PoiFilter filter =
        new PoiFilter(
            null,
            List.of(area.getId()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            5);
    var page = home.search(filter, null);
    assertEquals(12, page.total());
    assertEquals(5, page.items().size());
    assertEquals("Địa điểm 00", page.items().getFirst().name());
    assertEquals(1, page.items().getFirst().categories().size());
    assertNotNull(page.items().getFirst().imageUrl());
    assertTrue(
        statistics.getPrepareStatementCount() <= 6,
        "Số truy vấn tải thẻ phải được giới hạn, thực tế: "
            + statistics.getPrepareStatementCount());
    em.clear();
    statistics.clear();
    var markers = home.map(filter);
    assertEquals(12, markers.items().size());
    assertTrue(
        statistics.getPrepareStatementCount() <= 5,
        "Không được phát sinh một truy vấn cho từng POI");
  }

  @Test void keywordFindsCategoryAndAddressWithoutAccentsAndKeepsRadius() {
    Area area = new Area();area.setCode("SPEC_"+UUID.randomUUID().toString().replace("-", "").toUpperCase());area.setName("Vùng thử tìm kiếm");em.persist(area);
    PoiCategory category=new PoiCategory();category.setName("Cà phê");em.persist(category);
    Poi near=new Poi();near.setName("Vườn gần");near.setAddress("Đường Nguyễn Văn Linh");near.setArea(area);near.setCoordinates(16.001,108);near.addCategory(category);em.persist(near);
    Poi far=new Poi();far.setName("Vườn xa");far.setAddress("Đường Nguyễn Văn Linh");far.setArea(area);far.setCoordinates(16.06,108);far.addCategory(category);em.persist(far);
    em.flush();em.clear();
    PoiFilter coffee=new PoiFilter("coffee",List.of(area.getId()),null,null,null,null,null,16.0,108.0,2000,null,null,null,null,"distance",0,20);
    var matches=home.search(coffee,null);assertEquals(1,matches.total());assertEquals(near.getId(),matches.items().getFirst().id());
    PoiFilter expanded=new PoiFilter("coffee",List.of(area.getId()),null,null,null,null,null,16.0,108.0,10000,null,null,null,null,"distance",0,20);
    assertEquals(2,home.search(expanded,null).total());
    PoiFilter address=new PoiFilter("nguyen van linh",List.of(area.getId()),null,null,null,null,null,null,null,null,null,null,null,null,null,0,20);
    assertEquals(2,home.search(address,null).total());
    assertEquals(1,home.map(coffee).total());
  }
}
