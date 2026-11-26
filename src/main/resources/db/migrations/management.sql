-- Apply to an existing database after Hibernate has created the management tables.
-- New installations also use this script for the indexes below.
BEGIN;
ALTER TABLE certifications ALTER COLUMN expires_on DROP NOT NULL;
CREATE INDEX IF NOT EXISTS ix_pois_management ON pois(business_ref_id, publication_status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_products_management ON products(business_ref_id, publication_status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_certifications_management ON certifications(business_ref_id, publication_status, expires_on);
CREATE INDEX IF NOT EXISTS ix_tours_management ON tours(business_ref_id, publication_status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_stories_management ON stories(business_ref_id, publication_status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_events_management ON tourism_events(business_ref_id, publication_status, starts_at);
CREATE INDEX IF NOT EXISTS ix_members_user_business ON business_members(user_id, business_id);
CREATE INDEX IF NOT EXISTS ix_media_business_created ON media_assets(business_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_media_links_asset ON media_links(asset_id);
CREATE INDEX IF NOT EXISTS ix_audits_target ON management_audits(target_type, target_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_listing_poi_product ON product_listings(poi_ref_id, product_ref_id);
COMMIT;
