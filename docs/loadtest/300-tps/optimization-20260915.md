# 300 TPS optimization — 2026-09-15

## Applied change

`NotificationPersistenceAdapter.save()` no longer calls `saveAndFlush()` for every notification state transition. It now uses `repository.save()` and lets the surrounding transaction flush once at commit.

Expected effect: fewer JDBC round-trips and lower commit/flush pressure on `notification` and `event_publication` writes.

## Evidence

The latest baseline SQL snapshot showed repeated notification updates and inserts, a high `COMMIT` digest cost, 29–31 active database connections, and 6,780 outstanding event publications. The change targets the unnecessary explicit flush on this hot path without changing delivery semantics.

## Verification

- `./gradlew.bat test`: passed
- Shell and Node validation: passed
- Candidate image build/push: blocked because the local Docker Desktop Linux engine was unavailable.

The image was subsequently built and pushed after starting Docker Desktop. The candidate was then run under the identical 300 TPS workload. It was rejected: 161.90 req/s, p95 8.32 s, p99 12.36 s, and 14.35% failures versus the baseline's 161.55 req/s, p95 7.44 s, p99 9.73 s, and 11.68% failures. The candidate was not promoted and the source was restored to `saveAndFlush()`.

## Next run

Build and push the source as `ai-saveflush-20260915`, then run:

```bash
IMAGE_TAGS=ai-saveflush-20260915 \
TARGET_RPS=300 STAGE_DURATION=10m TEST_PLAN=single \
MYSQL_ROOT_PASSWORD='***' bash loadtest/run-ai-optimization.sh
```

Accept the candidate only if the mixed workload reaches 300 req/s with p95 ≤500 ms, p99 ≤1 s, request failures <1%, and no growing event backlog.
