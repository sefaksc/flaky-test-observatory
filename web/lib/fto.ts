export type TopFlakyItem = {
  testCaseId: string;
  className: string;
  testName: string;
  runs: number;
  fails: number;
  passes: number;
  failRate: number;
  flips: number;
  lastSeenAt: string | null;
};

export type TopFlakyResponse = {
  items: TopFlakyItem[];
  page: number;
  size: number;
  hasNext: boolean;
  windowDays: number;
  minRuns: number;
  project: string;
};

export const pct = (x: number) => `${Math.round(x * 1000) / 10}%`;

export const fmtDate = (iso: string | null) => {
  if (!iso) return "-";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
};
