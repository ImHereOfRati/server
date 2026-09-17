import workload from './mixed-workload.js';

// Each current workload iteration issues exactly one business HTTP request.
const rate = Number(__ENV.TARGET_RPS || 100);
const warmup = Number(__ENV.WARMUP_SECONDS || 120);
const duration = Number(__ENV.MEASURE_SECONDS || 600);
// Allocate the full bounded pool up front: dynamic VU initialization can drop
// arrivals during transient latency spikes even when maxVUs has headroom.
const vus = Number(__ENV.PREALLOCATED_VUS || 1000);
const maxVus = Number(__ENV.MAX_VUS || 1000);
const ramp = (__ENV.RAMP || 'false') === 'true';
const rampMax = Number(__ENV.RAMP_MAX_RPS || 300);
const warmupPlan = (__ENV.WARMUP_RATES || '30:120,60:60,90:60,120:60,145:60,165:120')
  .split(',').map(item => item.split(':').map(Number));
const warmupTotal = warmupPlan.reduce((sum, [, seconds]) => sum + seconds, 0);
if (!warmupPlan.every(([r, seconds]) => Number.isInteger(r) && Number.isInteger(seconds) && r > 0 && seconds > 0)) {
  throw new Error('WARMUP_RATES must be comma-separated rate:seconds pairs');
}
if (![rate, warmup, duration, vus, maxVus].every(Number.isInteger) ||
    rate <= 0 || (!ramp && warmup <= 0) || duration <= 0 || vus <= 0 || maxVus < vus) {
  throw new Error('Rate, warmup, duration and VUs must be positive integers; maxVUs must cover preAllocatedVUs');
}
const executor = (r, seconds, startTime) => ({
  executor: 'constant-arrival-rate', rate: r, timeUnit: '1s',
  duration: `${seconds}s`, startTime, preAllocatedVUs: vus,
  maxVUs: maxVus, gracefulStop: '30s',
});
const rampStages = ramp ? [
  { target: 165, duration: '2m' },
  ...Array.from({ length: Math.max(0, Math.floor((rampMax - 165) / 10)) }, (_, i) => ({
    target: 175 + i * 10, duration: '2m',
  })),
] : [];
export const options = {
  insecureSkipTLSVerify: true,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
  scenarios: {
    ...(ramp ? { measured: {
      executor: 'ramping-arrival-rate', startRate: 100, timeUnit: '1s',
      preAllocatedVUs: vus, maxVUs: maxVus, stages: rampStages, gracefulStop: '30s',
    } } : {
      warmup: {
        executor: 'ramping-arrival-rate', startRate: warmupPlan[0][0], timeUnit: '1s',
        preAllocatedVUs: vus, maxVUs: maxVus,
        stages: warmupPlan.map(([target, seconds]) => ({ target, duration: `${seconds}s` })),
        gracefulStop: '30s',
      },
      // Allow the staged warmup requests to finish before the measurement starts.
      measured: executor(rate, duration, `${warmupTotal + 30}s`),
    }),
  },
  thresholds: {
    'http_req_failed{scenario:measured}': [ramp ? 'rate<0.05' : 'rate<0.05', {
      threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: ramp ? '30s' : `${warmup + 60}s`,
    }],
    'checks{scenario:measured}': ['rate>0.99'],
    'http_req_duration{scenario:measured}': ['p(95)<500', 'p(99)<1000'],
    'dropped_iterations{scenario:measured}': ['count==0'],
    ...(ramp ? {} : {
      'loadtest_http_request_count{scenario:measured}': [`count>=${Math.floor(rate * duration * 0.99)}`],
      'loadtest_success_count{scenario:measured}': [`count>=${Math.floor(rate * duration * 0.99)}`],
    }),
    'loadtest_server_error_count{scenario:measured}': ['count==0'],
    'loadtest_network_error_count{scenario:measured}': ['count==0'],
    ...(ramp ? {} : {
      'http_req_failed{scenario:warmup}': ['rate<0.01'],
      'dropped_iterations{scenario:warmup}': ['count==0'],
      'loadtest_server_error_count{scenario:warmup}': ['count==0'],
      'loadtest_network_error_count{scenario:warmup}': ['count==0'],
    }),
  },
};
export default workload;
export function handleSummary(data) {
  const result = {
    config: { rate, warmupSeconds: ramp ? 0 : warmupTotal, warmupPlan: ramp ? [] : warmupPlan,
      measureSeconds: ramp ? rampStages.map(s => s.target) : duration,
      scenario: __ENV.SCENARIO || 'mixed', preAllocatedVUs: vus,
      startedAt: __ENV.RUN_STARTED_AT, imageDigest: __ENV.IMAGE_DIGEST },
    ...data,
  };
  return { [__ENV.RESULT_PATH || 'stability-summary.json']: JSON.stringify(result, null, 2) };
}
