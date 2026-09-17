#!/usr/bin/env node
import { readFile, writeFile } from 'node:fs/promises';

const [input, output] = process.argv.slice(2);
if (!output) throw Error('usage: summarize-observations.mjs <observations.jsonl> <report.md>');
const samples = (await readFile(input, 'utf8')).trim().split('\n').map(JSON.parse);
function metric(text, name) {
  const values = text.split('\n').filter(line => line.startsWith(`${name}{`) || line.startsWith(`${name} `))
    .map(line => Number(line.split(' ').at(-1)));
  return values.length ? Math.max(...values) : null;
}
const rows = samples.map(s => {
  const db = s.db.split('\n');
  return { at: s.at, phase: s.phase, events: /^\d+$/.test(db[0]) ? Number(db[0]) : null,
    notifications: /^\d+$/.test(db[1]) ? Number(db[1]) : null,
    pending: metric(s.app, 'hikaricp_connections_pending'), timeouts: metric(s.app, 'hikaricp_connections_timeout_total'),
    active: metric(s.app, 'hikaricp_connections_active'), queue: metric(s.app, 'executor_queued_tasks'),
    cpu: metric(s.app, 'process_cpu_usage'), systemCpu: metric(s.app, 'system_cpu_usage'),
    lifecycle: s.app.split('\n').find(line => line.startsWith('restart=')) || 'missing' };
});
const firstRecovery = rows.find(r => r.phase === 'recovery');
const drained = rows.find(r => r.phase === 'recovery' && r.events === 0 && r.notifications === 0 && r.pending === 0);
const missing = rows.some(r => [r.events, r.notifications, r.pending, r.timeouts, r.cpu].includes(null));
const timeoutDelta = rows.at(-1).timeouts === null || rows[0].timeouts === null ? null : rows.at(-1).timeouts - rows[0].timeouts;
const lines = ['# Server observations', '',
  `- Samples: ${rows.length}; missing essential values: ${missing}`,
  `- Hikari timeout delta: ${timeoutDelta}`,
  `- Maximum Hikari pending: ${Math.max(...rows.map(r => r.pending ?? 0))}`,
  `- Maximum incomplete events: ${Math.max(...rows.map(r => r.events ?? 0))}`,
  `- Backlog zero observed after first recovery snapshot: ${drained ? ((Date.parse(drained.at) - Date.parse(firstRecovery.at)) / 1000).toFixed(1) + 's' : 'not observed'}`,
  '- Recovery timestamps are relative to the first snapshot after k6 termination; polling/duration overhead must be considered at the 60s boundary.',
  '- Executor queue column includes the scheduler; consult raw samples for individual executor queues.', '',
  '| UTC | Phase | Events | Pending deliveries | Hikari active | Hikari pending | Timeouts | Process CPU | Host CPU | Lifecycle |',
  '|---|---|---:|---:|---:|---:|---:|---:|---:|---|'];
for (const r of rows) lines.push(`| ${r.at} | ${r.phase} | ${r.events} | ${r.notifications} | ${r.active} | ${r.pending} | ${r.timeouts} | ${r.cpu?.toFixed(3)} | ${r.systemCpu?.toFixed(3)} | ${r.lifecycle} |`);
await writeFile(output, lines.join('\n') + '\n');
