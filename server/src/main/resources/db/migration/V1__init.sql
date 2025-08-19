create extension if not exists pgcrypto;

-- Core schema for MVP
create table if not exists projects (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  token text not null, -- simple project token (dev only; later SSO/RBAC)
  created_at timestamptz not null default now()
);

create table if not exists test_runs (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  ci_job_id text,
  commit_sha text,
  branch text,
  started_at timestamptz not null,
  duration_ms int,
  status text not null,
  env_hash text
);

create table if not exists test_cases (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  suite text,
  name text,
  file text,
  avg_duration_ms int default 0,
  flakes_30d int default 0,
  last_flaky_at timestamptz
);

create table if not exists test_results (
  id uuid primary key default gen_random_uuid(),
  run_id uuid not null references test_runs(id) on delete cascade,
  case_id uuid not null references test_cases(id) on delete cascade,
  status text not null,
  duration_ms int,
  error_hash text,
  retries int default 0,
  artifacts_url text
);

create table if not exists flakes (
  id uuid primary key default gen_random_uuid(),
  case_id uuid not null references test_cases(id) on delete cascade,
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now(),
  flakiness_score numeric not null
);

create table if not exists rules (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  pattern text not null,
  action text not null, -- 'retry'|'quarantine'|'notify'
  params jsonb default '{}'::jsonb,
  ttl_days int default 14,
  enabled boolean default true
);

create table if not exists audit_log (
  id bigserial primary key,
  project_id uuid not null references projects(id) on delete cascade,
  actor text not null,
  action text not null,
  details jsonb,
  ts timestamptz not null default now()
);
