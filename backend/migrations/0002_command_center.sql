CREATE TABLE conversations (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES projects(id),
    title TEXT NOT NULL,
    lead_brain_id TEXT NOT NULL,
    state TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE conversation_messages (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL REFERENCES conversations(id),
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    provider_id TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE execution_plans (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL REFERENCES conversations(id),
    goal TEXT NOT NULL,
    plan_json TEXT NOT NULL,
    status TEXT NOT NULL,
    cost_class TEXT NOT NULL CHECK (cost_class IN ('FREE', 'PREPAID')),
    approved_at TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE bridge_requests (
    id TEXT PRIMARY KEY,
    task_id TEXT NOT NULL REFERENCES tasks(id),
    conversation_id TEXT NOT NULL REFERENCES conversations(id),
    provider_id TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    response_at TEXT
);

CREATE TABLE resource_ledger (
    provider_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    cost_class TEXT NOT NULL CHECK (cost_class IN ('FREE', 'PREPAID')),
    actual_cost_cents INTEGER NOT NULL DEFAULT 0 CHECK (actual_cost_cents = 0),
    quota TEXT NOT NULL,
    source_evidence TEXT NOT NULL,
    card_required INTEGER NOT NULL DEFAULT 0 CHECK (card_required = 0),
    human_gated INTEGER NOT NULL,
    last_verified_at TEXT NOT NULL
);

CREATE TABLE radar_resources (
    id TEXT PRIMARY KEY,
    source_id TEXT NOT NULL,
    title TEXT NOT NULL,
    source_url TEXT NOT NULL,
    version TEXT NOT NULL,
    cost_class TEXT NOT NULL DEFAULT 'FREE' CHECK (cost_class = 'FREE'),
    license TEXT NOT NULL,
    source_evidence TEXT NOT NULL,
    verified_at TEXT NOT NULL
);

CREATE TABLE evidence_bundles (
    id TEXT PRIMARY KEY,
    task_id TEXT NOT NULL,
    run_id TEXT NOT NULL,
    lead_brain TEXT NOT NULL,
    worker_provider TEXT NOT NULL,
    bundle_json TEXT NOT NULL,
    actual_cost_cents INTEGER NOT NULL DEFAULT 0 CHECK (actual_cost_cents = 0),
    created_at TEXT NOT NULL
);

INSERT INTO resource_ledger
    (provider_id, display_name, cost_class, actual_cost_cents, quota, source_evidence, card_required, human_gated, last_verified_at)
VALUES
    ('official-radar', 'Official Source Radar', 'FREE', 0, 'Public endpoint limits', 'https://docs.github.com/rest/releases/releases', 0, 0, CURRENT_TIMESTAMP),
    ('chatgpt', 'ChatGPT', 'PREPAID', 0, 'Subscription limits', 'https://platform.openai.com/docs/quickstart', 0, 1, CURRENT_TIMESTAMP),
    ('gemini', 'Gemini', 'PREPAID', 0, 'Subscription limits', 'Human-gated declaration', 0, 1, CURRENT_TIMESTAMP),
    ('deepseek', 'DeepSeek', 'FREE', 0, 'App limits', 'Human-gated declaration', 0, 1, CURRENT_TIMESTAMP);
