#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';


const outDir = path.resolve('synthetic');
fs.mkdirSync(outDir, { recursive: true });


function rand(n=1){ return Math.random()*n; }
function pick(arr){ return arr[Math.floor(Math.random()*arr.length)]; }


const suites = [
{ name: 'CalcSuite', tests: ['divByZero','sumLarge','mulPerf'] },
{ name: 'AuthSuite', tests: ['loginWorks','logoutWorks','refreshToken'] },
{ name: 'CartSuite', tests: ['addItem','removeItem','checkout'] },
];


const runs = 8; // kaç farklı run üretelim
for (let r=0; r<runs; r++) {
const ts = new Date(Date.now() - (runs-r)*3600*1000).toISOString();
let cases = [];
for (const s of suites) {
for (const t of s.tests) {
const pass = Math.random() > 0.35; // ~%65 pass
const dur = (50 + rand(250))/1000; // 0.05s..0.3s
if (pass) {
cases.push(`<testcase classname="${s.name}" name="${t}" time="${dur.toFixed(3)}"/>`);
} else {
cases.push(`<testcase classname="${s.name}" name="${t}" time="${dur.toFixed(3)}"><failure>AssertionError</failure></testcase>`);
}
}
}
const failures = cases.filter(c => c.includes('<failure>')).length;
const xml = `<?xml version="1.0" encoding="UTF-8"?>\n` +
`<testsuite name="SyntheticSuite" tests="${cases.length}" failures="${failures}" errors="0" skipped="0" time="1.0" timestamp="${ts}">\n` +
cases.join('\n') + `\n</testsuite>`;
const file = path.join(outDir, `run-${String(r+1).padStart(2,'0')}.xml`);
fs.writeFileSync(file, xml);
console.log(file);
}
console.log(`\nGenerated ${runs} JUnit XML files under ${outDir}`);