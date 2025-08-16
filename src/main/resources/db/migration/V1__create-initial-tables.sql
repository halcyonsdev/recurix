CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS subscriptions_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users
(
    id            BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
    telegram_id   BIGINT UNIQUE NOT NULL,
    first_name    VARCHAR(255),
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_telegram_id ON users (telegram_id);

CREATE TABLE IF NOT EXISTS subscriptions
(
    id             BIGINT PRIMARY KEY DEFAULT nextval('subscriptions_id_seq'),
    user_id        BIGINT NOT NULL,
    name           VARCHAR(255) NOT NULL,
    price          DECIMAL(10, 2) NOT NULL,
    currency       VARCHAR(3) NOT NULL,
    payment_date   DATE NOT NULL,
    renewal_months INT NOT NULL,
    category       VARCHAR(100),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),


    CONSTRAINT fk_subscriptions_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);
