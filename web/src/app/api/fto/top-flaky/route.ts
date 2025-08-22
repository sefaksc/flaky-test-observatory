import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  const backend = process.env.FTO_SERVER_URL ?? "http://localhost:8080";
  const url = new URL(req.url);
  const search = url.searchParams.toString();

  const headers: Record<string, string> = {};
  const token = req.headers.get("x-project-token");
  if (token) headers["X-Project-Token"] = token;

  try {
    const upstream = await fetch(`${backend}/api/top-flaky?${search}`, {
      method: "GET",
      headers,
      // Next 15: no-store (revalidate=0) etkisi için:
      cache: "no-store",
    });

    // Upstream 4xx/5xx ise body'deki hata mesajını da geçir
    const text = await upstream.text();
    let payload: any = undefined;
    try {
      payload = text ? JSON.parse(text) : undefined;
    } catch {
      /* not json */
    }
    if (!payload)
      payload = { error: text || upstream.statusText || "Upstream error" };

    return NextResponse.json(payload, { status: upstream.status });
  } catch (err: any) {
    // Backend tamamen ulaşılamıyorsa 503 dön
    return NextResponse.json(
      { error: "Backend unreachable", detail: String(err?.message ?? err) },
      { status: 503 },
    );
  }
}
