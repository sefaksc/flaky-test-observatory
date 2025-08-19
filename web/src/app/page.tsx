'use client';

import { useEffect, useState } from 'react';

type Health = { status: string; time?: string };

export default function Home() {
  const [health, setHealth] = useState<Health | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const api = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
    fetch(`${api}/health`)
      .then((r) => r.json())
      .then((d) => setHealth(d))
      .catch((e) => setError(String(e)));
  }, []);

  return (
    <main style={{ padding: 24, fontFamily: 'sans-serif' }}>
      <h1>Flaky Test Observatory</h1>
      <p>Bu sayfa, backend sağlığını kontrol eder ve sonucu gösterir.</p>

      {!health && !error && <p>Yükleniyor…</p>}
      {health && (
        <div style={{ marginTop: 12, padding: 12, border: '1px solid #ddd', borderRadius: 8 }}>
          <strong>Backend:</strong> {health.status.toUpperCase()}
          {health.time && <div>Zaman: {new Date(health.time).toLocaleString()}</div>}
        </div>
      )}
      {error && (
        <div style={{ color: 'crimson', marginTop: 12 }}>
          Backend'e bağlanırken hata: {error}
        </div>
      )}
    </main>
  );
}
