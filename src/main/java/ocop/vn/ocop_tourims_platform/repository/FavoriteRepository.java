package ocop.vn.ocop_tourims_platform.repository;

import java.util.*;
import ocop.vn.ocop_tourims_platform.entity.UserFavoritePoi;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<UserFavoritePoi, UUID> {
  @Query(
      "select f from UserFavoritePoi f join f.poi p left join p.business b where f.user.id=:userId"
          + " and p.active=true and (b is null or b.active=true)")
  Page<UserFavoritePoi> findByUserIdAndPoiActiveTrue(
      @Param("userId") UUID userId, Pageable pageable);

  @Query("select f.poi.id from UserFavoritePoi f where f.user.id = :user and f.poi.id in :pois")
  Set<UUID> favoriteIds(@Param("user") UUID user, @Param("pois") Collection<UUID> pois);

  @Modifying
  @Query(
      value =
          "INSERT INTO user_favorite_pois (id,user_ref_id,poi_ref_id,created_at,updated_at) VALUES"
              + " (gen_random_uuid(),:user,:poi,now(),now()) ON CONFLICT (user_ref_id,poi_ref_id)"
              + " DO NOTHING",
      nativeQuery = true)
  void add(@Param("user") UUID user, @Param("poi") UUID poi);

  @Modifying
  @Query("delete from UserFavoritePoi f where f.user.id=:user and f.poi.id=:poi")
  void remove(@Param("user") UUID user, @Param("poi") UUID poi);
}
