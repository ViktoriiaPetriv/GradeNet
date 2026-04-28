-- Ініціалізація баз даних для GradeNet (виконується тільки при першому створенні кластера)

CREATE DATABASE org_db;
CREATE DATABASE user_db;
CREATE DATABASE grade_db;

GRANT ALL PRIVILEGES ON DATABASE org_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE user_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE grade_db TO CURRENT_USER;

-- Перевірка
SELECT 'org_db created' AS status;
SELECT 'user_db created' AS status;
SELECT 'grade_db created' AS status;
