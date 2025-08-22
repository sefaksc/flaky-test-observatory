"use client";

import * as React from "react";
import {
  Container,
  Stack,
  TextField,
  Button,
  Alert,
  Pagination,
  Typography,
  Paper,
} from "@mui/material";
import TopFlakyTable from "../../../components/TopFlakyTable";
import type { TopFlakyResponse } from "../../../lib/fto";

export default function FlakyPage() {
  const [project, setProject] = React.useState("demo");
  const [token, setToken] = React.useState("");
  const [windowDays, setWindowDays] = React.useState(30);
  const [minRuns, setMinRuns] = React.useState(1);
  const [size, setSize] = React.useState(20);
  const [page, setPage] = React.useState(1);

  const [data, setData] = React.useState<TopFlakyResponse | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const qs = React.useMemo(() => {
    const sp = new URLSearchParams();
    if (!token) sp.set("project", project);
    sp.set("windowDays", String(windowDays));
    sp.set("minRuns", String(minRuns));
    sp.set("page", String(page));
    sp.set("size", String(size));
    return sp.toString();
  }, [project, token, windowDays, minRuns, page, size]);

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
        } catch {
          /* body yok veya json değil */
        }
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
            label="Token (X-Project-Token)"
            value={token}
            onChange={(e) => resetPage(() => setToken(e.target.value))}
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
