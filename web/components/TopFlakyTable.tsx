"use client";
import * as React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  LinearProgress,
  Box,
} from "@mui/material";
import { TopFlakyItem, pct, fmtDate } from "../lib/fto";

type Props = { items: TopFlakyItem[] };
export default function TopFlakyTable({ items }: Props) {
  if (!items || items.length === 0) {
    return (
      <Paper
        elevation={0}
        sx={{
          p: 2,
          borderRadius: 2,
          border: "1px solid",
          borderColor: "divider",
        }}
      >
        Kayıt bulunamadı.
      </Paper>
    );
  }
  return (
    <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
      <Table size="small" stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell>Test</TableCell>
            <TableCell align="right">Runs</TableCell>
            <TableCell align="right">Fails</TableCell>
            <TableCell align="right">Passes</TableCell>
            <TableCell align="right">Fail Rate</TableCell>
            <TableCell align="right">Flips</TableCell>
            <TableCell>Last Seen</TableCell>
            <TableCell align="right">Score</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((it) => (
            <TableRow key={it.testCaseId} hover>
              <TableCell>
                <div style={{ fontWeight: 600 }}>{it.testName}</div>
                <div style={{ opacity: 0.7 }}>{it.className}</div>
              </TableCell>
              <TableCell align="right">{it.runs}</TableCell>
              <TableCell align="right">
                <Chip
                  size="small"
                  label={it.fails}
                  color={it.fails > 0 ? "error" : "default"}
                  variant={it.fails > 0 ? "filled" : "outlined"}
                />
              </TableCell>
              <TableCell align="right">{it.passes}</TableCell>
              <TableCell align="right">{pct(it.failRate)}</TableCell>
              <TableCell align="right">{it.flips}</TableCell>
              <TableCell>{fmtDate(it.lastSeenAt)}</TableCell>
              <TableCell align="right">
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <span>{pct(it.score)}</span>
                  <Box sx={{ flex: 1 }}>
                    <LinearProgress
                      variant="determinate"
                      value={Math.max(0, Math.min(100, it.score * 100))}
                    />
                  </Box>
                </Box>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
