-- Rename column (no FK — specialty_offering lives in org_db, a separate database)
ALTER TABLE book_number RENAME COLUMN specialty_id TO specialty_offering_id;
