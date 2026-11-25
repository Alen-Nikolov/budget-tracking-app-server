ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
-- accounts that existed before verification was introduced stay usable
UPDATE users SET email_verified = TRUE;

CREATE TABLE email_verification_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    client     VARCHAR(20) NOT NULL,   -- WEB or ANDROID: decides where the link redirects afterwards
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_verification_user_id ON email_verification_tokens (user_id);
