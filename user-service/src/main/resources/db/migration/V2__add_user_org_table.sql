CREATE TABLE user_organizations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES user_app (id) ON DELETE CASCADE,
    org_id      BIGINT      NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_organization UNIQUE (user_id, org_id)
);