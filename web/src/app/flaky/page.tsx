"use client";

import React from "react";
import { TopFlakyResponse } from "../../../lib/fto";
import { Container, Stack } from "@mui/system";
import {
  Alert,
  Button,
  MenuItem,
  Pagination,
  Paper,
  TextField,
  Typography,
  CircularProgress,
} from "@mui/material";
import Autocomplete, { createFilterOptions } from "@mui/material/Autocomplete";
import TopFlakyTable from "../../../components/TopFlakyTable";
import { SORT_FIELDS, SortDir, SortField } from "../../../lib/sort";
import { BranchItem } from "../../../lib/branchItem";

export default function FlakyPage() {
  const [project, setProject] = React.useState("demo");
  const [token, setToken] = React.useState("");
  const [windowDays, setWindowDays] = React.useState(30);
  const [minRuns, setMinRuns] = React.useState(1);
  const [size, setSize] = React.useState(20);
  const [page, setPage] = React.useState(1);
  const [branch, setBranch] = React.useState("");
  const [q, setQ] = React.useState("");
  const [sort, setSort] = React.useState<SortField>("score");
  const [dir, setDir] = React.useState<SortDir>("desc");

  const [data, setData] = React.useState<TopFlakyResponse | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [branchList, setBranchList] = React.useState<BranchItem[]>([]);
  const [branchLoading, setBranchLoading] = React.useState(false);
  const [branchError, setBranchError] = React.useState<string | null>(null);

  const allBranchOption: BranchItem = { name: "", runs: 0 };
  const branchOptions = React.useMemo(
    () => [allBranchOption, ...branchList],
    [branchList],
  );

  React.useEffect(() => {
    let alive = true;
    async function loadBranches() {
      setBranchLoading(true);
      setBranchError(null);
      try {
        const sp = new URLSearchParams({
          project,
          windowDays: String(windowDays),
        });
        const res = await fetch(`/api/fto/branches?${sp.toString()}`, {
          headers: token ? { "X-Project-Token": token } : undefined,
          cache: "no-store",
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const list = (await res.json()) as BranchItem[];
        if (!alive) return;
        setBranchList(list);
        // seçili branch listede yoksa temizle
        if (branch && !list.some((b) => b.name === branch)) {
          setBranch("");
        }
      } catch (e: any) {
        if (!alive) return;
        setBranchError(e?.message ?? String(e));
        setBranchList([]);
      } finally {
        if (alive) setBranchLoading(false);
      }
    }
    loadBranches();
    return () => {
      alive = false;
    };
  }, [project, token, windowDays]);

  const qs = React.useMemo(() => {
    const sp = new URLSearchParams();
    if (!token) sp.set("project", project);
    sp.set("windowDays", String(windowDays));
    sp.set("minRuns", String(minRuns));
    sp.set("page", String(page));
    sp.set("size", String(size));
    if (branch) sp.set("branch", branch);
    if (q) sp.set("q", q);
    sp.set("sort", sort);
    sp.set("dir", dir);
    return sp.toString();
  }, [project, token, windowDays, minRuns, page, size, branch, q, sort, dir]);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/fto/top-flaky?${qs}`, {
        headers: token ? { "X-Project-Token": token } : undefined,
        cache: "no-store",
      });
      if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try {
          const j = await res.json();
          if (j?.error) msg += ` – ${j.error}`;
        } catch {}
        throw new Error(msg);
      }
      const json = (await res.json()) as TopFlakyResponse;
      setData(json);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    load();
  }, [qs, token]);
  const resetPage = (fn: () => void) => {
    setPage(1);
    fn();
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Top Flaky
      </Typography>

      <Paper sx={{ p: 2, mb: 2, borderRadius: 2 }}>
        <Stack
          direction={{ xs: "column", md: "row" }}
          spacing={2}
          alignItems="center"
        >
          <TextField
            label="Project"
            value={project}
            onChange={(e) => resetPage(() => setProject(e.target.value))}
            disabled={!!token}
            size="small"
          />
          <TextField
            label="Token"
            value={token}
            onChange={(e) => resetPage(() => setToken(e.target.value))}
            size="small"
          />
          <Autocomplete
            size="small" // <- yükseklikleri eşitle
            disablePortal // (opsiyonel) portal kullanmasın
            options={branchOptions}
            value={
              branchOptions.find((b) => b.name === branch) ?? allBranchOption
            }
            isOptionEqualToValue={(o, v) => o.name === v.name}
            getOptionLabel={(o) => o?.name || "(All branches)"}
            loading={branchLoading}
            onChange={(_, val) => resetPage(() => setBranch(val?.name ?? ""))}
            filterOptions={createFilterOptions<BranchItem>({
              matchFrom: "any",
              ignoreAccents: true,
              stringify: (o) => `${o.name} ${o.runs}`,
            })}
            sx={{ minWidth: 240 }} // diğer inputlarla uyumlu genişlik
            renderOption={(props, option) => (
              <li
                {...props}
                key={option.name || "(all)"}
                style={{ display: "flex", width: "100%" }}
              >
                <span style={{ flex: 1 }}>
                  {option.name || "(All branches)"}
                </span>
              </li>
            )}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Branch (search)"
                placeholder="Search branch…"
                size="small"
                // helperText'i sadece gerçekten gerekince göster → fazladan boşluk yok
                helperText={
                  branchError ? `Branches: ${branchError}` : undefined
                }
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <>
                      {branchLoading ? <CircularProgress size={16} /> : null}
                      {params.InputProps.endAdornment}
                    </>
                  ),
                }}
              />
            )}
          />
          <TextField
            label="Search (q)"
            value={q}
            onChange={(e) => resetPage(() => setQ(e.target.value))}
            size="small"
          />
          <TextField
            label="Window (days)"
            type="number"
            inputProps={{ min: 1, max: 365 }}
            value={windowDays}
            onChange={(e) =>
              resetPage(() =>
                setWindowDays(
                  Math.max(1, Math.min(365, Number(e.target.value) || 1)),
                ),
              )
            }
            size="small"
          />
          <TextField
            label="Min Runs"
            type="number"
            inputProps={{ min: 1 }}
            value={minRuns}
            onChange={(e) =>
              resetPage(() =>
                setMinRuns(Math.max(1, Number(e.target.value) || 1)),
              )
            }
            size="small"
          />
          <TextField
            label="Page Size"
            type="number"
            inputProps={{ min: 1, max: 200 }}
            value={size}
            onChange={(e) =>
              resetPage(() =>
                setSize(
                  Math.max(1, Math.min(200, Number(e.target.value) || 20)),
                ),
              )
            }
            size="small"
          />
        </Stack>
        <Stack
          direction={{ xs: "column", md: "row" }}
          spacing={2}
          alignItems="center"
          sx={{ mt: 2 }}
        >
          <TextField
            select
            label="Sort"
            value={sort}
            onChange={(e) =>
              resetPage(() => setSort(e.target.value as SortField))
            }
            size="small"
            sx={{ minWidth: 160 }}
          >
            {SORT_FIELDS.map((o) => (
              <MenuItem key={o.value} value={o.value}>
                {o.label}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Dir"
            value={dir}
            onChange={(e) => resetPage(() => setDir(e.target.value as any))}
            size="small"
            sx={{ minWidth: 120 }}
          >
            <MenuItem value="desc">desc</MenuItem>
            <MenuItem value="asc">asc</MenuItem>
          </TextField>
          <Button variant="contained" onClick={load} disabled={loading}>
            {loading ? "Yükleniyor..." : "Yenile"}
          </Button>
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <TopFlakyTable items={data?.items ?? []} />

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mt: 2 }}
      >
        <Typography variant="body2">
          Sayfa {data?.page ?? page} • Boyut {data?.size ?? size}
        </Typography>
        <Pagination
          page={data?.page ?? page}
          onChange={(_, p) => setPage(p)}
          count={(data?.hasNext ? data!.page + 1 : data?.page) ?? 1}
          color="primary"
        />
      </Stack>
    </Container>
  );
}
