# SMS Recovery Chaos Test Report - 2026-09-04

## Purpose

Verify that SMS notifications left in `PROCESSING` by an application container crash are recovered after the application
restarts.

## Environment

| Item                 | Value                                           |
|----------------------|-------------------------------------------------|
| AWS region           | `ap-northeast-2`                                |
| CloudFormation stack | `imhere-loadtest`                               |
| App profile          | `prod,loadtest`                                 |
| SMS provider         | `LoadTestExternalMessageAdapter`                |
| k6 script            | `loadtest/k6/test/one-to-many-test/sms-send.js` |
| Target endpoint      | `POST /api/notifications`                       |
| Test phone number    | `01090000000`                                   |

The load-test profile does not call real Solapi. Provider delivery was verified with DB state and generated
`provider_message_id` values instead of the Solapi console.

## Procedure

1. Created the load-test AWS environment with `loadtest/setup/setup-loadtest.sh`.
2. Generated fixture users and JWTs with the load-test init scripts.
3. Set `LOADTEST_PROVIDER_DELAY_MS=30000` on the app container to make in-flight SMS delivery easier to interrupt.
4. Ran `sms-send.js` against the app EC2.
5. Executed `docker kill iamhere-server-container` while SMS requests were being processed.
6. Checked notification rows in MySQL.
7. Restarted the app container manually with `docker start iamhere-server-container`.
8. Waited for `NotificationRecoveryScheduler` and checked final notification state.
9. Deleted the load-test environment with `loadtest/setup/teardown-loadtest.sh --yes`.

## Result

Immediately after the container kill, 8 SMS notifications were left in `PROCESSING`.

| Range             | State after kill | Provider message id |
|-------------------|------------------|---------------------|
| `20003` - `20010` | `PROCESSING`     | `NULL`              |

After restart and recovery, all 8 target rows became `SENT` and each row had a generated `provider_message_id`.

| Range             | Final state |
|-------------------|-------------|
| `20003` - `20010` | `SENT`      |

The test phone number had `114` `SENT` SMS rows and `114` distinct provider message ids. Since the interrupted rows had
`provider_message_id=NULL` before recovery, this run did not show a duplicate provider id for the same notification. It
did create multiple SMS notifications to the same phone number because the k6 scenario intentionally submits repeated
requests to the same target.

## Findings

- Message recovery worked for the crash-injected `PROCESSING` SMS rows.
- The expected "5 minutes after restart" recovery timing was not accurate for `PROCESSING` rows. The current code
  recovers stale `PROCESSING` rows with `now.minusMinutes(10)`.
- `findRecoverable` uses a 5-minute threshold for `PENDING` and `FAILED`, but `recoverStalled` uses a 10-minute
  threshold for `PROCESSING`.
- `docker kill` left the app container in `Exited (137)` during this run. It did not restart automatically, so the test
  used `docker start` to satisfy the restart condition.

## Cleanup

`teardown-loadtest.sh --yes` completed successfully. The `imhere-loadtest` CloudFormation stack no longer existed after
cleanup, and the generated EC2 key pair was deleted.
