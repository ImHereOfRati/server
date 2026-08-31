import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'https://127.0.0.1').replace(/\/$/, '');
const users = new SharedArray('imhere-only-one-fcm-user', () =>
  JSON.parse(open(__ENV.FIXTURE || '../../generated/tokens.json')).users,
);
const actor = users[Number(__ENV.ACTOR_INDEX || 0)];
if (!actor) throw new Error('No fixture user selected');

const statuses = new Counter('only_one_fcm_response_count');
const rateLimited = new Counter('only_one_fcm_rate_limit_count');
const serverErrors = new Counter('only_one_fcm_server_error_count');
const networkErrors = new Counter('only_one_fcm_network_error_count');
const duration = new Trend('only_one_fcm_request_duration', true);
const stageDuration = __ENV.STAGE_DURATION || '10s';

export const options = {
  insecureSkipTLSVerify: (__ENV.INSECURE_TLS || 'true') === 'true',
  scenarios: {
    one_user_fcm: {
      executor: 'ramping-arrival-rate',
      startRate: 200,
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 300),
      maxVUs: Number(__ENV.MAX_VUS || 1000),
      stages: [
        { target: 200, duration: stageDuration },
        { target: 300, duration: stageDuration },
        { target: 400, duration: stageDuration },
      ],
    },
  },
  thresholds: {},
};

export default function () {
  const response = http.post(
    `${baseUrl}/api/notifications`,
    JSON.stringify({
      notificationMethod: 'FCM',
      // FCM은 수신 대상을 사용자 id로 지정한다. SMS는 phone을 쓴다.
      targetIds: [actor.id],
      type: 'ARRIVAL',
      extraData: { placeName: 'loadtest', body: 'rate limit probe' },
    }),
    {
      headers: {
        Authorization: `Bearer ${actor.accessToken}`,
        'Content-Type': 'application/json',
      },
      tags: { endpoint: 'only-one-fcm' },
    },
  );
  const status = response.status || 0;
  statuses.add(1, { status: String(status) });
  duration.add(response.timings.duration);
  if (status === 429) rateLimited.add(1);
  if (status >= 500) serverErrors.add(1);
  if (status === 0) networkErrors.add(1);
  check(response, { 'only-one FCM request completed': (r) => r.status !== 0 });
}
