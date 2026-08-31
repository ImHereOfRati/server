import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'https://127.0.0.1').replace(/\/$/, '');
const users = new SharedArray('imhere-only-one-user', () =>
  JSON.parse(open(__ENV.FIXTURE || '../../generated/tokens.json')).users,
);
const actorIndex = Number(__ENV.ACTOR_INDEX || 0);
const actor = users[actorIndex];

if (!actor) throw new Error(`No fixture user at ACTOR_INDEX=${actorIndex}`);

const responseCount = new Counter('only_one_response_count');
const rateLimitCount = new Counter('only_one_rate_limit_count');
const serverErrorCount = new Counter('only_one_server_error_count');
const gatewayUnavailableCount = new Counter('only_one_gateway_unavailable_count');
const upstreamServerErrorCount = new Counter('only_one_upstream_server_error_count');
const networkErrorCount = new Counter('only_one_network_error_count');
const requestDuration = new Trend('only_one_request_duration', true);
const plan = (__ENV.TEST_PLAN || 'precision').toLowerCase();
const stageDuration = __ENV.STAGE_DURATION || '3m';
const defaultStages = [
  { target: 200, duration: stageDuration },
  { target: 300, duration: stageDuration },
  { target: 400, duration: stageDuration },
];
const stages = plan === 'single'
  ? [{ target: Number(__ENV.TARGET_RPS || 100), duration: stageDuration }]
  : defaultStages;

export const options = {
  insecureSkipTLSVerify: (__ENV.INSECURE_TLS || 'true') === 'true',
  scenarios: {
    one_user: {
      executor: 'ramping-arrival-rate',
      startRate: stages[0].target,
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 300),
      maxVUs: Number(__ENV.MAX_VUS || 1000),
      stages,
    },
  },
  // 429 is expected after a per-user limiter is installed. Report statuses
  // instead of failing the run on expected throttling responses.
  thresholds: {},
};

export default function () {
  const response = http.get(`${baseUrl}/api/users/my`, {
    headers: {
      Authorization: `Bearer ${actor.accessToken}`,
      'Content-Type': 'application/json',
    },
    tags: { endpoint: 'only-one-user-me' },
  });

  const status = response.status || 0;
  responseCount.add(1, { status: String(status) });
  requestDuration.add(response.timings.duration);
  if (status === 429) rateLimitCount.add(1);
  if (status >= 500) {
    serverErrorCount.add(1);
    if (status === 503) gatewayUnavailableCount.add(1);
    else upstreamServerErrorCount.add(1);
  }
  if (status === 0) networkErrorCount.add(1);

  check(response, { 'only-one request completed': (r) => r.status !== 0 });
}
