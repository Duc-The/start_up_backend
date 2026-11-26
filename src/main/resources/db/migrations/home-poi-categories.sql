-- Apply after Hibernate has created the home entities.
-- Run against the intended database with psql -v ON_ERROR_STOP=1.
BEGIN;

DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pois' AND column_name = 'category_ref_id'
    ) THEN
        EXECUTE '
            INSERT INTO public.poi_category_links
                (id, poi_ref_id, category_ref_id, created_by, created_at, updated_by, updated_at)
            SELECT gen_random_uuid(), id, category_ref_id, created_by, created_at, updated_by, updated_at
            FROM public.pois WHERE category_ref_id IS NOT NULL
            ON CONFLICT (poi_ref_id, category_ref_id) DO NOTHING';
        ALTER TABLE public.pois DROP COLUMN category_ref_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'pois' AND column_name = 'address_type'
    ) THEN
        CREATE TEMP TABLE home_poi_roles ON COMMIT DROP AS
        SELECT id AS poi_id,
            CASE address_type
                WHEN 'ATTRACTION' THEN 'Điểm tham quan'
                WHEN 'RETAIL' THEN 'Điểm bán'
                WHEN 'MANUFACTURER' THEN 'Điểm sản xuất'
                WHEN 'BOTH' THEN 'Điểm sản xuất'
            END AS category_name
        FROM public.pois WHERE address_type <> 'UNKNOWN'
        UNION ALL
        SELECT id, 'Điểm bán' FROM public.pois WHERE address_type = 'BOTH';

        INSERT INTO public.poi_categories (id, name, created_at, updated_at)
        SELECT gen_random_uuid(), names.category_name, now(), now()
        FROM (SELECT DISTINCT category_name FROM home_poi_roles WHERE category_name IS NOT NULL) names
        WHERE NOT EXISTS (
            SELECT 1 FROM public.poi_categories c WHERE c.name = names.category_name
        );

        INSERT INTO public.poi_category_links
            (id, poi_ref_id, category_ref_id, created_at, updated_at)
        SELECT gen_random_uuid(), r.poi_id,
            (SELECT c.id FROM public.poi_categories c
             WHERE c.name = r.category_name ORDER BY c.id LIMIT 1), now(), now()
        FROM home_poi_roles r WHERE r.category_name IS NOT NULL
        ON CONFLICT (poi_ref_id, category_ref_id) DO NOTHING;

        ALTER TABLE public.pois DROP COLUMN address_type;
    END IF;
END
$migration$;

DO $constraints$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.product_types'::regclass
            AND conname = 'product_types_parent_check'
    ) THEN
        ALTER TABLE public.product_types ADD CONSTRAINT product_types_parent_check
            CHECK (parent_ref_id <> id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.product_types'::regclass
            AND conname = 'product_types_display_check'
    ) THEN
        ALTER TABLE public.product_types ADD CONSTRAINT product_types_display_check
            CHECK (sort_order >= 0 AND btrim(icon_url) <> '');
    END IF;
END
$constraints$;

COMMIT;
