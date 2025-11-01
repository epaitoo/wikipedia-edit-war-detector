CREATE TABLE edit_war_alerts (
    id BIGSERIAL PRIMARY KEY,
    page_title VARCHAR(500) NOT NULL,
    wiki VARCHAR(100) NOT NULL,

    -- Severity metrics
    severity_level VARCHAR(20) NOT NULL,
    severity_score DECIMAL(3,2) NOT NULL,

     -- Edit metrics
    total_edits INTEGER NOT NULL,
    conflict_edits INTEGER NOT NULL,
    conflict_ratio DECIMAL(3,2) NOT NULL,

    -- User information
    user_count INTEGER NOT NULL,
    involved_users TEXT NOT NULL, -- JSON array as text

    -- Timestamps
    first_edit_timestamp BIGINT NOT NULL,
    last_edit_timestamp BIGINT NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_alerts_detected_at ON edit_war_alerts(detected_at DESC);
CREATE INDEX idx_alerts_page_title ON edit_war_alerts(page_title);
CREATE INDEX idx_alerts_wiki ON edit_war_alerts(wiki);
CREATE INDEX idx_alerts_severity ON edit_war_alerts(severity_level);
CREATE INDEX idx_alerts_status ON edit_war_alerts(status);

-- Composite index for common query patterns
CREATE INDEX idx_alerts_wiki_status_detected ON edit_war_alerts(wiki, status, detected_at DESC);

-- Comments for documentation
COMMENT ON TABLE edit_war_alerts IS 'Stores detected edit war incidents';
COMMENT ON COLUMN edit_war_alerts.severity_score IS 'Calculated severity from 0.0 to 1.0';
COMMENT ON COLUMN edit_war_alerts.conflict_ratio IS 'Percentage of edits that are conflicts (0.0 to 1.0)';

-- Edit Events Table (for analytics and historical tracking)
CREATE TABLE edit_events (
    id BIGSERIAL PRIMARY KEY,

    -- Page information
    page_title VARCHAR(500) NOT NULL,
    wiki VARCHAR(100) NOT NULL,
    namespace INTEGER NOT NULL,

    -- User information
    username VARCHAR(255) NOT NULL,
    is_bot BOOLEAN NOT NULL DEFAULT FALSE,

    -- Edit details
    length_old INTEGER,
    length_new INTEGER,
    length_change INTEGER,

    -- Metadata
    event_type VARCHAR(50) NOT NULL,
    timestamp BIGINT NOT NULL,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for analytics queries
CREATE INDEX idx_events_timestamp ON edit_events(timestamp DESC);
CREATE INDEX idx_events_page_title ON edit_events(page_title);
CREATE INDEX idx_events_wiki ON edit_events(wiki);
CREATE INDEX idx_events_username ON edit_events(username);
CREATE INDEX idx_events_is_bot ON edit_events(is_bot);

-- Composite index for page activity analysis
CREATE INDEX idx_events_page_timestamp ON edit_events(page_title, timestamp DESC);

COMMENT ON TABLE edit_events IS 'Stores individual edit events for analytics';