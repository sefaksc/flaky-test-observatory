import { NextRequest, NextResponse } from "next/server";

const SERVER = process.env.FTO_SERVER_URL ?? "http://localhost:8080";

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const project = url.searchParams.get("project") ?? "demo";
  const windowDays = url.searchParams.get("windowDays") ?? "30";
  const limit = url.searchParams.get("limit") ?? "200";
  const token = req.headers.get("x-project-token") ?? "";

  const upstream = new URL(`${SERVER}/api/branches`);
  upstream.searchParams.set("project", project);
  upstream.searchParams.set("windowDays", windowDays);
  upstream.searchParams.set("limit", limit);

  const res = await fetch(upstream.toString(), {
    headers: token ? { "X-Project-Token": token } : undefined,
    cache: "no-store",
  });

  const body = await res.text();
  return new NextResponse(body, {
    status: res.status,
    headers: {
      "content-type": res.headers.get("content-type") || "application/json",
    },
  });
}
