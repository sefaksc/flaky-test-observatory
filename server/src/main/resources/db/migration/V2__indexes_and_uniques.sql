-- Proje token'ı için benzersizlik (constraint) - güvenli şekilde ekle
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'projects_token_uniq'
  ) THEN
    ALTER TABLE projects
      ADD CONSTRAINT projects_token_uniq UNIQUE (token);
  END IF;
END$$;

-- Aynı projede (suite, name, file) kombinasyonunu benzersiz yap (NULL'ları '' gibi ele al)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_test_cases_project_suite_name_file'
  ) THEN
    CREATE UNIQUE INDEX ux_test_cases_project_suite_name_file
      ON test_cases (
        project_id,
        (COALESCE(suite, '')),
        name,
        (COALESCE(file, ''))
      );
  END IF;
END$$;

-- Idempotent import için hash sütunu
ALTER TABLE test_runs
  ADD COLUMN IF NOT EXISTS import_hash text;

-- (project_id, import_hash) benzersiz olsun
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_test_runs_project_hash'
  ) THEN
    CREATE UNIQUE INDEX ux_test_runs_project_hash
      ON test_runs (project_id, import_hash);
  END IF;
END$$;

-- Sorgu hızları için indeksler
CREATE INDEX IF NOT EXISTS ix_test_results_case ON test_results (case_id);
CREATE INDEX IF NOT EXISTS ix_test_results_run  ON test_results (run_id);
