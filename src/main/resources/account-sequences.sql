-- Run before Hibernate creates the account tables on a fresh database.
CREATE SEQUENCE IF NOT EXISTS users_user_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS roles_role_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS user_roles_user_role_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS user_identities_user_identity_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS poi_categories_category_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS pois_poi_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_geometries_geometry_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS product_types_product_type_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS products_product_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS product_listings_product_listing_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS product_images_product_image_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS certifications_certification_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS tours_tour_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS tour_stops_tour_stop_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS tour_route_segments_tour_route_segment_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS orders_order_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS order_items_order_item_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS payments_payment_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS payments_payos_order_code_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS poi_reviews_poi_review_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_review_media_poi_review_media_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS product_reviews_product_review_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS product_review_media_product_review_media_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS areas_area_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS activities_activity_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS amenities_amenity_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_category_links_poi_category_link_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_activities_poi_activity_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_amenities_poi_amenity_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS poi_media_poi_media_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS user_favorite_pois_favorite_poi_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS notifications_notification_id_seq START WITH 100001;

CREATE SEQUENCE IF NOT EXISTS businesses_business_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS business_members_business_member_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS stories_story_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS tourism_events_event_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS media_assets_media_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS media_links_media_link_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS management_audits_audit_id_seq START WITH 100001;
CREATE SEQUENCE IF NOT EXISTS import_batches_import_id_seq START WITH 100001;
