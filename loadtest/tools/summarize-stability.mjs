#!/usr/bin/env node
import { createReadStream } from 'node:fs';
import { readFile, writeFile } from 'node:fs/promises';
import { createInterface } from 'node:readline';

const [pointsPath, summaryPath, outputPath] = process.argv.slice(2);
if (!outputPath) throw Error('usage: summarize-stability.mjs <points.json> <summary.json> <report.md>');
const summary = JSON.parse(await readFile(summaryPath, 'utf8'));
const bins = new Map(), statuses = new Map(), errors = new Map();
let origin;
for await (const line of createInterface({ input: createReadStream(pointsPath), crlfDelay: Infinity })) {
  let point;
  try { point = JSON.parse(line); } catch { continue; }
  if (point.type !== 'Point') continue;
  const { time, value, tags } = point.data;
  const timestamp = Date.parse(time);
  if (origin === undefined) origin = timestamp;
  if (tags?.scenario !== 'measured') continue;
  const seconds = (timestamp - origin) / 1000 - summary.config.warmupSeconds - 30;
  const minute = Math.max(0, Math.floor(seconds / 60));
  if (!bins.has(minute)) bins.set(minute, { requests: 0, success: 0, failures: 0, dropped: 0, durations: [] });
  const bin = bins.get(minute);
  if (point.metric === 'http_reqs') bin.requests += value;
  if (point.metric === 'loadtest_success_count') bin.success += value;
  if (point.metric === 'http_req_failed') bin.failures += value;
  if (point.metric === 'dropped_iterations') bin.dropped += value;
  if (point.metric === 'http_req_duration') bin.durations.push(value);
  if (point.metric === 'loadtest_response_count') {
    const key = `${tags.endpoint} / ${tags.status}`;
    statuses.set(key, (statuses.get(key) || 0) + value);
  }
  if (point.metric === 'loadtest_network_error_count' && value) errors.set(tags.error_code, (errors.get(tags.error_code) || 0) + value);
}
const metric = (name, key) => summary.metrics[`${name}{scenario:measured}`]?.values?.[key] ?? 0;
const observed = Math.max(0, summary.state.testRunDurationMs / 1000 - summary.config.warmupSeconds - 30);
const completed = observed >= summary.config.measureSeconds;
const lines = [
  '# Stability measurement', '',
  `- Target: ${summary.config.rate} RPS; workload: ${summary.config.scenario}`,
  `- Warmup: ${summary.config.warmupSeconds}s + 30s grace; requested measurement: ${summary.config.measureSeconds}s`,
  `- Actual total run: ${(summary.state.testRunDurationMs / 1000).toFixed(2)}s; measurement including drain: ${observed.toFixed(2)}s`,
  `- Completed scheduled measurement: ${completed}`,
  `- Successful responses: ${metric('loadtest_success_count', 'count')}`,
  `- Successful responses / scheduled seconds: ${(metric('loadtest_success_count', 'count') / summary.config.measureSeconds).toFixed(2)} req/s (aborted runs are not steady throughput estimates)`,
  `- HTTP failure rate: ${(metric('http_req_failed', 'rate') * 100).toFixed(3)}%`,
  `- p95: ${metric('http_req_duration', 'p(95)').toFixed(2)}ms; p99: ${metric('http_req_duration', 'p(99)').toFixed(2)}ms`,
  `- Dropped iterations: ${metric('dropped_iterations', 'count')}`, '',
  '## Minute windows', '',
  'Completion-time windows relative to the first k6 point and configured measurement start. Final window can be partial or contain draining requests. Requests/60 is not an actual rate for a partial window.', '',
  '| Minute | Requests | Successful | HTTP failures | p95 ms | Dropped |',
  '|---|---:|---:|---:|---:|---:|',
];
for (const [minute, bin] of [...bins.entries()].sort((a, b) => a[0] - b[0])) {
  if (!bin.requests && !bin.success && !bin.failures && !bin.dropped && !bin.durations.length) continue;
  bin.durations.sort((a, b) => a - b);
  const p95 = bin.durations[Math.max(0, Math.ceil(bin.durations.length * 0.95) - 1)] || 0;
  lines.push(`| ${minute + 1} | ${bin.requests} | ${bin.success} | ${bin.failures} | ${p95.toFixed(2)} | ${bin.dropped} |`);
}
lines.push('', '## Endpoint / status', '', '| Endpoint / status | Count |', '|---|---:|');
for (const [key, count] of [...statuses.entries()].sort()) lines.push(`| ${key} | ${count} |`);
lines.push('', '## Network error codes', '', JSON.stringify(Object.fromEntries(errors)), '',
  'Server observations and recovery must also pass before accepting a stable capacity.', '');
await writeFile(outputPath, lines.join('\n'));
