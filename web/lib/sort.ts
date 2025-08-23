// ---- Sort seçenekleri ve tipleri ----
export const SORT_FIELDS = [
  { label: "Score", value: "score" },
  { label: "Flips", value: "flips" },
  { label: "Fail Rate", value: "failRate" },
  { label: "Runs", value: "runs" },
  { label: "Last Seen", value: "lastSeen" },
] as const;

export type SortField = (typeof SORT_FIELDS)[number]["value"];
export type SortDir = "asc" | "desc";
