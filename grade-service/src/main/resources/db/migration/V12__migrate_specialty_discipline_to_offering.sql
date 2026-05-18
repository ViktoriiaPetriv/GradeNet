-- Drop existing constraint and index
ALTER TABLE specialty_discipline DROP CONSTRAINT IF EXISTS uq_specialty_discipline;
DROP INDEX IF EXISTS idx_sd_specialty;

-- Rename column (no FK — specialty_offering lives in org_db, a separate database)
ALTER TABLE specialty_discipline RENAME COLUMN specialty_id TO specialty_offering_id;

CREATE UNIQUE INDEX uq_specialty_discipline ON specialty_discipline(specialty_offering_id, discipline_id);
CREATE INDEX idx_sd_specialty_offering ON specialty_discipline(specialty_offering_id);
