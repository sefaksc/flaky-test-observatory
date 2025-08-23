CREATE INDEX IF NOT EXISTS ix_test_runs_project_branch_started
ON test_runs (project_id, branch, started_at);
