CREATE TABLE commission (
    id         BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL
);

CREATE TABLE commission_member (
    id            BIGSERIAL PRIMARY KEY,
    commission_id BIGINT  NOT NULL,
    professor_id  BIGINT  NOT NULL,
    is_head       BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_cm_commission FOREIGN KEY (commission_id) REFERENCES commission (id) ON DELETE CASCADE,
    CONSTRAINT uq_cm_commission_professor UNIQUE (commission_id, professor_id)
);

CREATE INDEX idx_cm_commission ON commission_member (commission_id);
