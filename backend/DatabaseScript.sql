CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    profile_picture VARCHAR(500),
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE conversations(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    is_group BOOLEAN DEFAULT FALSE,
    group_picture VARCHAR(500),
    allow_participants_invite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE conversation_participants(
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    cleared_at TIMESTAMP,
    left_at TIMESTAMP NULL,
    is_admin BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages(
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    reply_to_id BIGINT REFERENCES messages(id) ON DELETE SET NULL,
    edited_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'sent',
    sent_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE message_reads(
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
);

CREATE TABLE message_deliveries(
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delivered_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
);

CREATE TABLE message_deletes(
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    deleted_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
);

CREATE EXTENSION vector;

CREATE TABLE message_embedding(
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    embedding vector(768),
    PRIMARY KEY (message_id)
);

CREATE TABLE password_reset_tokens(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE
);

CREATE TABLE blocked_users(
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE notifications(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE friend_requests(
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(sender_id, receiver_id),
    CHECK (sender_id <> receiver_id)
);

CREATE TABLE conversation_summaries(
    conversation_id BIGINT PRIMARY KEY REFERENCES conversations(id) ON DELETE CASCADE,
    summary_text TEXT NOT NULL,
    last_message_id BIGINT REFERENCES messages(id) ON DELETE SET NULL
);

CREATE TABLE user_summary_quota(
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    remaining_quota INT NOT NULL DEFAULT 5,
    last_reset_date DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE INDEX idx_messages_conversation
    ON messages(conversation_id, sent_at DESC);

CREATE INDEX idx_conversation_participants_user
    ON conversation_participants(user_id);

CREATE INDEX idx_message_reads_user
    ON message_reads(user_id);

CREATE INDEX idx_message_deliveries_user
    ON message_deliveries(user_id);

CREATE INDEX idx_notifications_user
    ON notifications(user_id, is_read);

CREATE INDEX idx_messages_sender
    ON messages(sender_id);

CREATE INDEX idx_blocked_users_blocked
    ON blocked_users(blocked_id);

CREATE INDEX ON message_embedding USING hnsw (embedding vector_cosine_ops);