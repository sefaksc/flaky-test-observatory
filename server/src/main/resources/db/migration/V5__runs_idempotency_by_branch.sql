-- Unique idempotency per project+hash (hash = body+branch+commit)
CREATE UNIQUE INDEX IF NOT EXISTS uq_test_runs_project_import_hash
ON test_runs (project_id, import_hash);
CREATE INDEX IF NOT EXISTS ix_test_runs_import_hash_branch
ON test_runs (project_id, import_hash, COALESCE(branch,''));