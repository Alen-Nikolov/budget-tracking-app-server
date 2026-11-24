CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    colour     VARCHAR(7)   NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

CREATE TABLE expenses (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id  BIGINT         NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    amount       NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    note         VARCHAR(255),
    expense_date DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);
-- serves the main "my expenses between two dates" query
CREATE INDEX idx_expenses_user_date ON expenses (user_id, expense_date);
