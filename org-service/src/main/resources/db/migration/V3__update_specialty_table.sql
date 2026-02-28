-- 1. Прибираємо унікальність з полів code, name_ua та name_en
ALTER TABLE specialty
    DROP CONSTRAINT IF EXISTS specialty_code_key;

ALTER TABLE specialty
    DROP CONSTRAINT IF EXISTS specialty_name_ua_key;

ALTER TABLE specialty
    DROP CONSTRAINT IF EXISTS specialty_name_en_key;

-- 2. Додаємо унікальний індекс на комбінацію code + degree + edu_type + org_id
CREATE UNIQUE INDEX uq_specialty_code_degree_eduType_org
    ON specialty (code, degree, edu_type, org_id);