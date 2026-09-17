#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { readFile, writeFile, mkdir, appendFile } from 'node:fs/promises';
import path from 'node:path';
import { createHash } from 'node:crypto';

// Uses an already provisioned isolated stack. Infrastructure lifecycle remains
// with setup-loadtest.sh / teardown-loadtest.sh; never targets a supplied URL.
const stack = process.env.STACK_NAME;
if (!/^imhere-loadtest-stability-[a-zA-Z0-9-]+$/.test(stack || '')) throw Error('An isolated stability stack is required');
const root = process.cwd();
const region = process.env.AWS_REGION || 'ap-northeast-2';
const dir = path.resolve(process.env.REPORT_DIR || `docs/loadtest/stability/${stack}`);
const raw = path.resolve('loadtest/k6/generated/stability', path.basename(dir));
await mkdir(dir, { recursive: true });
await mkdir(raw, { recursive: true });
const q = s => `'${String(s).replaceAll("'", "'\\''")}'`;
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
function command(exe, args, input) {
  return new Promise((resolve, reject) => {
    const child = spawn(exe, args, { windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
    let out = '', err = '';
    child.stdout.on('data', b => { out += b; });
    child.stderr.on('data', b => { err += b; });
    child.on('error', reject);
    child.on('close', code => code === 0 ? resolve(out) : reject(Error(`${path.basename(exe)} exited ${code}: ${err.slice(-1500)}`)));
    child.stdin.on('error', () => {});
    child.stdin.end(input);
  });
}
const aws = args => command('aws', [...args, '--region', region, '--output', 'json']);
await command('k6', ['inspect', 'loadtest/k6/test/many-people-many-request-test/stability.js']);
const info = JSON.parse(await aws(['cloudformation', 'describe-stacks', '--stack-name', stack])).Stacks[0];
if (info.StackStatus !== 'CREATE_COMPLETE') throw Error('Expected a fresh CREATE_COMPLETE stack');
const output = key => info.Outputs.find(o => o.OutputKey === key)?.OutputValue;
const app = output('AppPublicIp'), db = output('DbPublicIp');
const state = await readFile('loadtest/setup/.loadtest-state', 'utf8');
if (!state.split(/\r?\n/).includes(`STACK_NAME=${stack}`)) throw Error('State/stack mismatch');
let key = state.match(/^SSH_KEY_PATH=(.+)$/m)[1].trim();
key = key.replace(/^\/([a-z])\//, (_, drive) => `${drive.toUpperCase()}:/`);
const sshExe = process.platform === 'win32' ? 'C:/Program Files/Git/usr/bin/ssh.exe' : 'ssh';
const ssh = (host, cmd, input) => command(sshExe, ['-i', key, '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=10', '-o', 'ServerAliveInterval=10', '-o', 'ServerAliveCountMax=3', `ec2-user@${host}`, cmd], input);
const runtimeEnv = await ssh(app, 'cat /opt/imhere/env/server.env');
const password = runtimeEnv.match(/^DB_PASSWORD=(.+)$/m)?.[1].trim();
if (!password) throw Error('Isolated DB password missing');
const sql = query => ssh(db, `sudo env MYSQL_PWD=${q(password)} mysql --protocol=socket -uroot --batch --skip-column-names rati`, query);
const digest = (await ssh(app, "sudo docker inspect iamhere-server-container --format '{{.Image}}'")).trim();
const repoDigests = JSON.parse(await ssh(app, `sudo docker image inspect ${q(digest)} --format '{{json .RepoDigests}}'`));
const allowedSettings = /^(SPRING_PROFILES_ACTIVE|DB_POOL_SIZE|DB_CONNECTION_TIMEOUT_MS|LOADTEST_PROVIDER_DELAY_MS|LOADTEST_PROVIDER_FAILURE_RATE|LOADTEST_PROVIDER_FAILURE_MODE)=/;
const runtimeSettings = Object.fromEntries(runtimeEnv.split(/\r?\n/).filter(line => allowedSettings.test(line)).map(line => {
  const index = line.indexOf('='); return [line.slice(0, index), line.slice(index + 1)];
}));
const seedSha256 = createHash('sha256').update(await readFile('loadtest/k6/generated/seed.sql')).digest('hex');
await writeFile(path.join(dir, 'environment.json'), JSON.stringify({ stack, region, imageId: digest, repoDigests, runtimeSettings, seedSha256, driver: `${process.platform}/${process.arch}`, k6: (await command('k6', ['version'])).trim(), git: (await command('git', ['rev-parse', 'HEAD'])).trim(), createdAt: new Date().toISOString(), workload: 'mixed: reads 75%, FCM 15%, SMS 10%', note: 'Working tree harness changes; application image is retained across container restarts' }, null, 2));
const tokens = JSON.parse(await readFile('loadtest/k6/generated/tokens.json', 'utf8'));
const base = `http://${app}`;
async function ready() {
  for (let i = 0; i < 90; i++) {
    try {
      const res = await fetch(`${base}/api/users/my`, { headers: { Authorization: `Bearer ${tokens.users[0].accessToken}` }, signal: AbortSignal.timeout(3000) });
      if (res.status === 200) return;
    } catch {}
    await wait(2000);
  }
  throw Error('Authenticated readiness failed');
}
async function snapshot(file, phase) {
  const collected = await Promise.allSettled([
    sql("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL; SELECT COUNT(*) FROM notification WHERE status IN ('PENDING','PROCESSING'); SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_running','Threads_connected','Innodb_row_lock_waits');"),
    ssh(app, "ip=$(sudo docker inspect iamhere-server-container --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'); curl -fsS --max-time 5 http://$ip:4861/actuator/prometheus | grep -E '^(hikaricp_|process_cpu_usage|system_cpu_usage|jvm_memory_used_bytes|executor_)'; sudo docker inspect iamhere-server-container --format 'restart={{.RestartCount}} oom={{.State.OOMKilled}} status={{.State.Status}}'; sudo docker stats --no-stream --format '{{.Name}} {{.CPUPerc}} {{.MemUsage}}'"),
  ]);
  const item = { at: new Date().toISOString(), phase,
    db: collected[0].status === 'fulfilled' ? collected[0].value : 'COLLECTION_FAILED',
    app: collected[1].status === 'fulfilled' ? collected[1].value : 'COLLECTION_FAILED' };
  await appendFile(file, JSON.stringify(item) + '\n');
  return item;
}
const rows = [];
async function run(rate, label, mode = 'mixed') {
  if (existsSync(path.join(dir, `${label}.json`))) throw Error(`Result already exists: ${label}; select a new REPORT_DIR`);
  console.log(`${new Date().toISOString()} Preparing ${label} (${rate} RPS ${mode})`);
  await ssh(app, 'sudo docker stop iamhere-server-container');
  await sql(await readFile('loadtest/k6/generated/seed.sql', 'utf8'));
  await sql('ANALYZE TABLE users, friend_relations, notification, event_publication;');
  await ssh(app, 'sudo docker start iamhere-server-container');
  // Resolve the new upstream IP after restart.
  await ssh(app, 'sudo docker exec nginx-container nginx -s reload');
  await ready();
  const samples = path.join(dir, `${label}-observations.jsonl`);
  const initial = await snapshot(samples, 'before');
  if (!/^0\s+0\s/.test(initial.db)) throw Error('Initial backlog is not empty');
  const startedAt = new Date().toISOString();
  const result = path.join(dir, `${label}.json`);
  const args = ['run', '--quiet', '--out', `json=${path.join(raw, `${label}-points.json`)}`];
  for (const [k, v] of Object.entries({ BASE_URL: base, FIXTURE: path.join(root, 'loadtest/k6/generated/tokens.json'), TEST_PLAN: 'single', SCENARIO: mode,
    TARGET_RPS: rate, WARMUP_SECONDS: process.env.WARMUP_SECONDS || 120, WARMUP_RATES: process.env.WARMUP_RATES || '30:120,60:60,90:60,120:60,145:60,165:120', MEASURE_SECONDS: process.env.MEASURE_SECONDS || 600,
    RAMP: process.env.RAMP || 'false', RAMP_MAX_RPS: process.env.RAMP_MAX_RPS || '300',
    RESULT_PATH: result, RUN_STARTED_AT: startedAt, IMAGE_DIGEST: digest })) args.push('-e', `${k}=${v}`);
  args.push('loadtest/k6/test/many-people-many-request-test/stability.js');
  let finished = false, finishedAt;
  const task = command('k6', args).then(async out => { await writeFile(path.join(raw, `${label}.log`), out); return 0; }, async err => { await writeFile(path.join(raw, `${label}.log`), err.message); return 1; }).finally(() => { finishedAt = Date.now(); finished = true; });
  while (!finished) {
    await snapshot(samples, 'load');
    await wait(15000);
  }
  const code = await task;
  const loadEnded = new Date(finishedAt).toISOString();
  let recovered = false;
  const recoveryDeadline = finishedAt + 60000;
  for (let i = 0; i <= 4; i++) {
    if (Date.now() > recoveryDeadline) break;
    if (i) await wait(Math.max(0, Math.min(15000, recoveryDeadline - Date.now() - 4000)));
    const snap = await snapshot(samples, 'recovery');
    if (Date.parse(snap.at) <= recoveryDeadline && /^0\s+0\s/.test(snap.db) &&
        /hikaricp_connections_pending\{[^\n]*\} 0(?:\.0)?\n/.test(snap.app)) recovered = true;
  }
  await writeFile(path.join(dir, `${label}-sql.tsv`), await sql("SELECT DIGEST_TEXT,COUNT_STAR,ROUND(SUM_TIMER_WAIT/1000000000,2),SUM_ROWS_EXAMINED FROM performance_schema.events_statements_summary_by_digest WHERE SCHEMA_NAME='rati' ORDER BY SUM_TIMER_WAIT DESC LIMIT 20; SELECT status,COUNT(*) FROM notification GROUP BY status;"));
  // Keep detailed service logs in ignored generated output; they may contain user data.
  await ssh(app, `sudo docker logs --since ${q(startedAt)} iamhere-server-container > /tmp/stability-app.log 2>&1`);
  await writeFile(path.join(raw, `${label}-app.log`), await ssh(app, 'tail -c 2000000 /tmp/stability-app.log'));
  await writeFile(path.join(raw, `${label}-nginx.log`), await ssh(app, `sudo docker logs --since ${q(startedAt)} nginx-container 2>&1 | tail -c 2000000`));
  if (!existsSync(result)) throw Error(`k6 exited before exporting a summary; see ${path.join(raw, `${label}.log`)}. This is not a capacity measurement.`);
  const summary = JSON.parse(await readFile(result, 'utf8'));
  await command('node', ['loadtest/tools/summarize-stability.mjs', path.join(raw, `${label}-points.json`), result, path.join(dir, `${label}.md`)]);
  await command('node', ['loadtest/tools/summarize-observations.mjs', samples, path.join(dir, `${label}-server.md`)]);
  const metrics = summary.metrics;
  const m = (name, field) => metrics[`${name}{scenario:measured}`]?.values?.[field];
  const observations = (await readFile(samples, 'utf8')).trim().split('\n').map(JSON.parse);
  const timeout = s => Number(s.app.match(/hikaricp_connections_timeout_total\{[^\n]*\} (\S+)/)?.[1]);
  const serverGate = observations.every(s => s.db !== 'COLLECTION_FAILED' &&
    /hikaricp_connections_pending\{/.test(s.app) && /restart=0 oom=false status=running/.test(s.app)) &&
    timeout(observations.at(-1)) === timeout(observations[0]);
  const passed = code === 0 && recovered && serverGate;
  const scheduledSeconds = Array.isArray(summary.config.measureSeconds) ? summary.config.measureSeconds.length * 120 : summary.config.measureSeconds;
  const row = { label, rate, mode, startedAt, loadEnded, wallSeconds: summary.state.testRunDurationMs / 1000,
    successfulRps: (m('loadtest_success_count', 'count') || 0) / scheduledSeconds,
    failed: m('http_req_failed', 'rate'), p95: m('http_req_duration', 'p(95)'), p99: m('http_req_duration', 'p(99)'),
    dropped: m('dropped_iterations', 'count') || 0, recovered, serverGate, httpGate: code === 0, passed };
  rows.push(row);
  await writeFile(path.join(dir, 'suite.json'), JSON.stringify(rows, null, 2));
  console.log(JSON.stringify(row));
  return passed;
}
const rates = (process.env.RATES || '100,165,200').split(',').map(Number);
if (!rates.every(r => Number.isInteger(r) && r > 0 && r <= 1000)) throw Error('Rates must be integers between 1 and 1000');
let lastPass;
for (const rate of rates) {
  if (await run(rate, `mixed-${rate}-1`)) { lastPass = rate; continue; }
  await run(rate, `mixed-${rate}-2`);
  await run(rate, `reads-${rate}-diagnostic`, 'reads');
  break;
}
if (lastPass) await run(lastPass, `mixed-${lastPass}-confirm`);
console.log(`Suite complete. Review server observations before declaring stability. Reports: ${dir}`);
