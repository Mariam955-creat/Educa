-- Promotions (prix barré, comme Udemy & co.) et vidéo de démo d'environ une minute, visible avant l'achat.

ALTER TABLE courses ADD COLUMN promo_price        NUMERIC(10, 2);
ALTER TABLE courses ADD COLUMN promo_ends_at      TIMESTAMPTZ;
ALTER TABLE courses ADD COLUMN preview_video_key  VARCHAR(500);
ALTER TABLE courses ADD COLUMN preview_video_type VARCHAR(100);

-- Une promotion est toujours strictement moins chère que le prix normal, et jamais gratuite
ALTER TABLE courses ADD CONSTRAINT chk_courses_promo_price
    CHECK (promo_price IS NULL OR (promo_price > 0 AND promo_price < price));
