import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'https://127.0.0.1').replace(/\/$/, '');
// The verification fixture is intentionally limited to 1,000 users.
const users = new SharedArray('imhere-loadtest-users', () =>
  JSON.parse(open(__ENV.FIXTURE || '../../generated/tokens.json')).users,
);
const mode = (__ENV.SCENARIO || 'mixed').toLowerCase();
const plan = (__ENV.TEST_PLAN || 'precision').toLowerCase();
const isBreakpoint = plan === 'breakpoint';
const responseCounts = new Counter('loadtest_response_count');
const conflictCount = new Counter('loadtest_conflict_count');
const serverErrorCount = new Counter('loadtest_server_error_count');
const networkErrorCount = new Counter('loadtest_network_error_count');
const measuredHttpRequestCount = new Counter('loadtest_http_request_count');
const measuredIterationCount = new Counter('loadtest_iteration_count');
const businessDuration = new Trend('loadtest_business_duration', true);

const stages = plan === 'precision'
  ? Array.from({ length: 10 }, (_, index) => ({
      target: 100 + index * 100,
      duration: '3m',
    }))
  : [
      { target: 30, duration: '3m' },
      { target: 60, duration: '3m' },
      { target: 100, duration: '3m' },
      { target: 150, duration: '3m' },
      { target: 225, duration: '3m' },
      { target: 300, duration: '3m' },
      { target: 400, duration: '3m' },
      { target: 600, duration: '3m' },
      { target: 800, duration: '3m' },
      { target: 1000, duration: '3m' },
    ];
if (plan === 'single') {
  stages.splice(0, stages.length, {
    target: Number(__ENV.TARGET_RPS || 100),
    duration: __ENV.STAGE_DURATION || '3m',
  });
}

export const options = {
  insecureSkipTLSVerify: (__ENV.INSECURE_TLS || 'true') === 'true',
  scenarios: {
    workload: {
      executor: 'ramping-arrival-rate',
      startRate: Number(__ENV.START_RPS || (plan === 'precision' ? 100 : 30)),
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 300),
      maxVUs: Number(__ENV.MAX_VUS || 2000),
      stages,
    },
  },
  thresholds: isBreakpoint ? {} : { http_req_failed: ['rate<0.01'] },
};

function user(index) { return users[Math.abs(index) % users.length]; }
function headers(token) { return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } }; }
function actor() { return user((__VU * 17 + __ITER) % users.length); }
function get(path, token) { return http.get(`${baseUrl}${path}`, headers(token)); }
function post(path, body, token) { return http.post(`${baseUrl}${path}`, JSON.stringify(body), headers(token)); }
function patch(path, body, token) { return http.patch(`${baseUrl}${path}`, JSON.stringify(body), headers(token)); }
function del(path, token) { return http.del(`${baseUrl}${path}`, null, headers(token)); }
function observe(response, name) {
  const status = response.status || 0;
  measuredHttpRequestCount.add(1, { endpoint: name });
  responseCounts.add(1, { endpoint: name, status: String(status) });
  businessDuration.add(response.timings.duration, { endpoint: name });
  if (status === 0) networkErrorCount.add(1, { endpoint: name });
  if (status === 409) conflictCount.add(1, { endpoint: name });
  if (status >= 500) serverErrorCount.add(1, { endpoint: name });
}

function ok(response, expected, name) {
  observe(response, name);
  return check(response, { [`${name} status ${expected}`]: (r) => r.status === expected });
}

function readWorkload(me) {
  const choice = (__VU + __ITER) % 10;
  if (choice < 2) return ok(get('/api/users/my', me.accessToken), 200, 'user-me');
  if (choice < 3) return ok(get(`/api/users?keyword=loadtest-${choice}`, me.accessToken), 200, 'user-search');
  if (choice < 5) return ok(get('/api/friendships?page=0&size=20', me.accessToken), 200, 'friendships');
  if (choice < 6) return ok(get('/api/friends/requests?type=RECEIVED&page=0&size=20', me.accessToken), 200, 'friend-requests');
  return ok(get('/api/notifications?page=0&size=20', me.accessToken), 200, 'notifications');
}

function friendMutation(me) {
  readWorkload(me);
}

function directNotification(me, notificationMethod) {
  const target = user((__VU + __ITER + 1) % users.length);
  const body = {
    notificationMethod,
    targetIds: [notificationMethod === 'FCM' ? target.id : target.phone],
    type: 'ARRIVAL',
    extraData: { placeName: 'loadtest', body: 'load test notification' },
  };
  ok(post('/api/notifications', body, me.accessToken), 202, `${notificationMethod.toLowerCase()}-send`);
}

export default function () {
  const me = actor();
  measuredIterationCount.add(1, { scenario: mode });
  if (mode === 'reads') readWorkload(me);
  else if (mode === 'friends') readWorkload(me);
  else if (mode === 'fcm') directNotification(me, 'FCM');
  else if (mode === 'sms') directNotification(me, 'SMS');
  else if (mode === 'breakpoint') {
    const choice = (__VU + __ITER) % 4;
    if (choice === 0) readWorkload(me);
    else if (choice === 1) directNotification(me, 'FCM');
    else if (choice === 2) directNotification(me, 'SMS');
    else readWorkload(me);
  }
  else {
    const choice = (__VU + __ITER) % 20;
    if (choice < 12) readWorkload(me);
    else if (choice < 15) readWorkload(me);
    else if (choice < 18) directNotification(me, 'FCM');
    else directNotification(me, 'SMS');
  }
  sleep(Number(__ENV.THINK_TIME || 0.05));
}
