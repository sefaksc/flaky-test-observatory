-- Pencere sorgularını hızlandırmak için temel indeksler
CREATE INDEX IF NOT EXISTS idx_test_runs_project_started
ON test_runs(project_id, started_at DESC);


CREATE INDEX IF NOT EXISTS idx_test_results_test_case_run
ON test_results(case_id, run_id);


CREATE INDEX IF NOT EXISTS idx_test_results_status
ON test_results(status);