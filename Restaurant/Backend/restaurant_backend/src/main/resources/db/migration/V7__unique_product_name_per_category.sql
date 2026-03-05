-- Enforce uniqueness per category for DE/EN names (case-insensitive, trimmed)
-- This prevents race-conditions that service-level checks cannot fully avoid.

create unique index if not exists ux_products_category_name_de_ci
    on products (category, lower(btrim(name_de)));

create unique index if not exists ux_products_category_name_en_ci
    on products (category, lower(btrim(name_en)));