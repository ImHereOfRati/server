#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises';

const [inputPath, outputPath, testName, plan, targetRps, duration] = process.argv.slice(2);
if (!inputPath || !outputPath) {
  console.error('usage: render-k6-report.mjs <summary.json> <report.md> <test> <plan> <target-rps> <duration>');
  process.exit(2);
}

const summary = JSON.parse(await readFile(inputPath, 'utf8'));
const metrics = summary.metrics ?? {};
const metric = (name, key = 'value') => metrics[name]?.[key] ?? 'n/a';
const pct = (name, key) => {
  const value = metric(name, key);
  return typeof value === 'number' ? `${value.toFixed(2)} ms` : value;
};
const generatedAt = new Date().toISOString();

const lines = [
  `# Load test: ${testName} / ${plan}`,
  '',
  `- Generated: ${generatedAt}`,
  `- Target RPS: ${targetRps}`,
  `- Stage duration: ${duration}`,
  `- Source summary: ${inputPath}`,
  '',
  '## Result',
  '',
  '| Metric | Value |',
  '|---|---:|',
  `| HTTP requests | ${metric('http_reqs', 'count')} |`,
  `| HTTP request rate | ${metric('http_reqs', 'rate')} req/s |`,
  `| HTTP duration p50 | ${pct('http_req_duration', 'med')} |`,
  `| HTTP duration p95 | ${pct('http_req_duration', 'p(95)')} |`,
  `| HTTP duration p99 | ${pct('http_req_duration', 'p(99)')} |`,
  `| HTTP failed rate | ${metric('http_req_failed', 'value')} |`,
  `| Max VUs | ${metric('vus_max')} |`,
  `| Dropped iterations | ${metric('dropped_iterations', 'count')} |`,
  '',
  '## Custom workload metrics',
  '',
  '| Metric | Count / rate |',
  '|---|---:|',
  `| Measured HTTP requests | ${metric('loadtest_http_request_count', 'count')} / ${metric('loadtest_http_request_count', 'rate')} req/s |`,
  `| Server errors | ${metric('loadtest_server_error_count', 'count')} / ${metric('loadtest_server_error_count', 'rate')} req/s |`,
  `| Network errors | ${metric('loadtest_network_error_count', 'count')} / ${metric('loadtest_network_error_count', 'rate')} req/s |`,
  `| Conflicts | ${metric('loadtest_conflict_count', 'count')} / ${metric('loadtest_conflict_count', 'rate')} req/s |`,
  '',
  '## Automated judgement',
  '',
  'This file records measurements only. The AI optimization runner must combine it with Hikari, MySQL, nginx, and async-backlog snapshots before accepting a candidate.',
  '',
];

await writeFile(outputPath, `${lines.join('\n')}\n`, 'utf8');
