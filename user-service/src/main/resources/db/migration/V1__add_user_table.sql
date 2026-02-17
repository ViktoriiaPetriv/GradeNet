CREATE TABLE user_app (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    patronymic VARCHAR(100),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    birth_date DATE         NOT NULL,
    role       VARCHAR(255)    NOT NULL
);
