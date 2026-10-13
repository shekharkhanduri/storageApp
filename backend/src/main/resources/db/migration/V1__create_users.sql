CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email              VARCHAR(255) UNIQUE NOT NULL,
    full_name          VARCHAR(100) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    avatar_url         VARCHAR(500),
    refresh_token_hash VARCHAR(255),
    created_at         TIMESTAMP DEFAULT NOW()
);
