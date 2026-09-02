PRAGMA foreign_keys = ON;

CREATE TABLE devices (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    public_key_spki_base64 TEXT NOT NULL,
    build_channel TEXT NOT NULL,
    created_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL
);

CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE tasks (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES projects(id),
    title TEXT NOT NULL,
    payload TEXT NOT NULL,
    source TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX tasks_updated_at_idx ON tasks(updated_at DESC);

CREATE TABLE task_runs (
    id TEXT PRIMARY KEY,
    task_id TEXT NOT NULL REFERENCES tasks(id),
    provider_id TEXT,
    status TEXT NOT NULL,
    started_at TEXT,
    completed_at TEXT,
    evidence_key TEXT
);

CREATE TABLE decisions (
    id TEXT PRIMARY KEY,
    task_id TEXT NOT NULL REFERENCES tasks(id),
    action TEXT,
    status TEXT NOT NULL,
    signature_base64 TEXT,
    signed_payload TEXT,
    created_at TEXT NOT NULL,
    decided_at TEXT
);

CREATE TABLE providers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    cost_class TEXT NOT NULL CHECK (cost_class IN ('FREE', 'PREPAID', 'METERED')),
    state TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 0,
    notes TEXT NOT NULL
);

CREATE TABLE candidate_builds (
    id TEXT PRIMARY KEY,
    task_id TEXT REFERENCES tasks(id),
    version_name TEXT NOT NULL,
    sha256 TEXT,
    evidence_key TEXT,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE radar_findings (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);

INSERT INTO projects (id, name, description, created_at)
VALUES ('hassan-ai', 'Hassan AI', 'Milestone 1 control plane', CURRENT_TIMESTAMP);

INSERT INTO providers (id, name, cost_class, state, enabled, notes) VALUES
('local', 'Local Device', 'FREE', 'READY', 1, 'Local Android and test runtime'),
('chatgpt', 'ChatGPT / Codex', 'PREPAID', 'HUMAN_GATE', 0, 'Manual bridge only'),
('gemini', 'Gemini', 'PREPAID', 'HUMAN_GATE', 0, 'Manual bridge only'),
('deepseek', 'DeepSeek', 'FREE', 'HUMAN_GATE', 0, 'No embedded API credentials'),
('metered', 'Metered APIs', 'METERED', 'BLOCKED', 0, 'Blocked by the free-only constitution');
