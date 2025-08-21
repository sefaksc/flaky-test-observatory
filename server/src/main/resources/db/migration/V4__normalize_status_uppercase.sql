-- 1) Boşlukları kırp
UPDATE test_results SET status = BTRIM(status) WHERE status IS NOT NULL;


-- 2) Eş anlamlıları kanonik değerlere çevir
UPDATE test_results SET status = 'FAILED'
WHERE UPPER(status) IN ('FAIL', 'FAILED', 'FAILURE', 'ERROR');
UPDATE test_results SET status = 'PASSED'
WHERE UPPER(status) IN ('PASS', 'PASSED', 'SUCCESS');
UPDATE test_results SET status = 'SKIPPED'
WHERE UPPER(status) IN ('SKIP', 'SKIPPED', 'IGNORE', 'IGNORED');


-- 3) Tam uppercase'e çek (muhtemel karışık değerler için genel güvence)
UPDATE test_results SET status = UPPER(status) WHERE status IS NOT NULL;


-- 4) CHECK constraint (yalnızca PASSED/FAILED/SKIPPED)
DO $$
BEGIN
IF NOT EXISTS (
SELECT 1 FROM information_schema.table_constraints
WHERE table_name = 'test_results' AND constraint_name = 'test_results_status_chk') THEN
ALTER TABLE test_results
ADD CONSTRAINT test_results_status_chk
CHECK (status IN ('PASSED','FAILED','SKIPPED'));
END IF;
END $$;