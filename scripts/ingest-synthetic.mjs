#!/usr/bin/env node
import { readdir, readFile } from 'node:fs/promises';
import { join, extname } from 'node:path';

const SERVER = process.env.FTO_SERVER_URL ?? 'http://localhost:8080';
const TOKEN  = process.env.FTO_PROJECT_TOKEN ?? 'dev-token-123';
const BRANCH = (process.argv.find(a => a.startsWith('--branch='))?.split('=')[1])
            ?? process.env.FTO_BRANCH ?? 'main';

async function main() {
  const dir = 'synthetic';
  let files = await readdir(dir);
  files = files.filter(f => extname(f).toLowerCase() === '.xml');
  if (files.length === 0) {
    console.error('No XML files under synthetic/');
    process.exit(1);
  }
  let ok = 0, fail = 0;
  for (const f of files) {
    const buf = await readFile(join(dir, f));
    const form = new FormData();
    const blob = new Blob([buf], { type: 'application/xml' });
    form.append('file', blob, f);

    const res = await fetch(`${SERVER}/ingest/junit`, {
      method: 'POST',
      headers: {
        'X-Project-Token': TOKEN,
        'X-Branch': BRANCH
      },
      body: form
    });

    if (res.ok) { ok++; process.stdout.write('.'); }
    else { fail++; process.stdout.write('x'); }
  }
  process.stdout.write('\n');
  console.log(`Ingest done: ${ok} ok, ${fail} failed • branch=${BRANCH} • server=${SERVER}`);
}

main().catch(err => { console.error(err); process.exit(1); });
