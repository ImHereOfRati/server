# SQL optimizer snapshot

- Collected: 2026-09-15T11:54:38Z
- Database: rati
- This report is for the isolated load-test database only.

## ANALYZE TABLE

```text
Table	Op	Msg_type	Msg_text
rati.users	analyze	status	OK
rati.friend_relations	analyze	status	OK
rati.notification	analyze	status	OK
rati.event_publication	analyze	status	OK
```

## Connection and lock status

```text
Variable_name	Value
Created_tmp_disk_tables	0
Created_tmp_tables	3
Innodb_row_lock_time	0
Innodb_row_lock_waits	0
Max_used_connections	31
Threads_connected	30
Threads_running	2
```

## Delivery and event backlog

```text
status	count
PENDING	3
SENT	21179
FAILED	13
outstanding_publications
14299
```

## Top statement digests

```text
DIGEST_TEXT	COUNT_STAR	total_ms	avg_ms	SUM_ROWS_EXAMINED
COMMIT	54800	120078.73	2191.22	0
SET `autocommit` = ?	109533	11348.13	103.60	0
INSERT INTO `event_publication` ( `completion_attempts` , `completion_date` , `event_type` , `last_resubmission_date` , `listener_id` , `publication_date` , `serialized_event` , STATUS , `id` ) VALUES (...)	15427	6835.30	443.07	0
SELECT `nje1_0` . `id` , `nje1_0` . `attempts` , `nje1_0` . `body` , `nje1_0` . `created_at` , `nje1_0` . `dedupe_key` , `nje1_0` . `extra_data` , `nje1_0` . `is_read` , `nje1_0` . `last_error` , `nje1_0` . `method` , `nje1_0` . `provider_message_id` , `nje1_0` . `provider_status` , `nje1_0` . `sender_alias` , `nje1_0` . `sent_at` , `nje1_0` . `status` , `nje1_0` . `target_identifier` , `nje1_0` . `title` , `nje1_0` . `type` , `nje1_0` . `updated_at` FROM `notification` `nje1_0` WHERE `nje1_0` . `target_identifier` = ? AND `nje1_0` . `method` = ? AND `nje1_0` . `status` = ? ORDER BY `nje1_0` . `created_at` DESC LIMIT ?	8492	6655.05	783.68	171630
SELECT `frje1_0` . `friend_relation_id` , `frje1_0` . `created_at` , `frje1_0` . `expired_at` , `frje1_0` . `high_alias` , `frje1_0` . `high_user_id` , `frje1_0` . `initiated_user_id` , `frje1_0` . `low_alias` , `frje1_0` . `low_user_id` , `frje1_0` . `message` , `frje1_0` . `status` , `frje1_0` . `updated_at` FROM `friend_relations` `frje1_0` WHERE ( `frje1_0` . `low_user_id` = ? OR `frje1_0` . `high_user_id` = ? ) AND `frje1_0` . `status` = ? ORDER BY `frje1_0` . `friend_relation_id` LIMIT ?, ...	8168	4931.04	603.70	8261
INSERT INTO `notification` ( `dedupe_key` , `target_identifier` , `method` , `sender_alias` , TYPE , `title` , `body` , `extra_data` , STATUS , `attempts` , `is_read` , `created_at` , `updated_at` ) VALUES ( ?, ... , NOW (?) , NOW (?) )	20000	3838.84	191.94	0
SET SESSION TRANSACTION READ ONLY	32151	3297.15	102.55	0
SET SESSION TRANSACTION READ WRITE	32208	3051.32	94.74	0
SELECT `uje1_0` . `id` , `uje1_0` . `created_at` , `uje1_0` . `email` , `uje1_0` . `nickname` , `uje1_0` . `oidc_subject` , `uje1_0` . `provider` , `uje1_0` . `refresh_token_version` , `uje1_0` . `role` , `uje1_0` . `status` , `uje1_0` . `updated_at` FROM `users` `uje1_0` WHERE `uje1_0` . `id` = ?	8187	2928.27	357.67	8189
SELECT `frje1_0` . `low_user_id` , `frje1_0` . `high_user_id` FROM `friend_relations` `frje1_0` WHERE `frje1_0` . `low_user_id` = ? OR `frje1_0` . `high_user_id` = ?	4183	2083.64	498.12	0
SELECT `uje1_0` . `id` , `uje1_0` . `created_at` , `uje1_0` . `email` , `uje1_0` . `nickname` , `uje1_0` . `oidc_subject` , `uje1_0` . `provider` , `uje1_0` . `refresh_token_version` , `uje1_0` . `role` , `uje1_0` . `status` , `uje1_0` . `updated_at` FROM `users` `uje1_0` WHERE `uje1_0` . `id` IN (...)	4134	1752.44	423.91	12397
SELECT `uje1_0` . `id` , `uje1_0` . `created_at` , `uje1_0` . `email` , `uje1_0` . `nickname` , `uje1_0` . `oidc_subject` , `uje1_0` . `provider` , `uje1_0` . `refresh_token_version` , `uje1_0` . `role` , `uje1_0` . `status` , `uje1_0` . `updated_at` FROM `users` `uje1_0` WHERE `uje1_0` . `nickname` = ? AND `uje1_0` . `status` = ? AND `uje1_0` . `id` NOT IN (...) ORDER BY `uje1_0` . `id` LIMIT ?, ...	2126	1412.19	664.25	2119
SELECT `uje1_0` . `id` , `uje1_0` . `created_at` , `uje1_0` . `email` , `uje1_0` . `nickname` , `uje1_0` . `oidc_subject` , `uje1_0` . `provider` , `uje1_0` . `refresh_token_version` , `uje1_0` . `role` , `uje1_0` . `status` , `uje1_0` . `updated_at` FROM `users` `uje1_0` WHERE `uje1_0` . `nickname` = ? AND `uje1_0` . `status` = ? AND `uje1_0` . `id` != ? ORDER BY `uje1_0` . `id` LIMIT ?, ...	2057	1200.72	583.72	2057
SELECT `frje1_0` . `friend_relation_id` , `frje1_0` . `created_at` , `frje1_0` . `expired_at` , `frje1_0` . `high_alias` , `frje1_0` . `high_user_id` , `frje1_0` . `initiated_user_id` , `frje1_0` . `low_alias` , `frje1_0` . `low_user_id` , `frje1_0` . `message` , `frje1_0` . `status` , `frje1_0` . `updated_at` FROM `friend_relations` `frje1_0` WHERE ( `frje1_0` . `low_user_id` = ? OR `frje1_0` . `high_user_id` = ? ) AND `frje1_0` . `status` = ? AND `frje1_0` . `initiated_user_id` != ? ORDER BY `frje1_0` . `friend_relation_id` LIMIT ?, ...	1996	1189.48	595.93	0
SELECT `nje1_0` . `id` , `nje1_0` . `attempts` , `nje1_0` . `body` , `nje1_0` . `created_at` , `nje1_0` . `dedupe_key` , `nje1_0` . `extra_data` , `nje1_0` . `is_read` , `nje1_0` . `last_error` , `nje1_0` . `method` , `nje1_0` . `provider_message_id` , `nje1_0` . `provider_status` , `nje1_0` . `sender_alias` , `nje1_0` . `sent_at` , `nje1_0` . `status` , `nje1_0` . `target_identifier` , `nje1_0` . `title` , `nje1_0` . `type` , `nje1_0` . `updated_at` FROM `notification` `nje1_0` WHERE `nje1_0` . `id` = ?	2400	1068.05	445.02	2401
UPDATE `notification` SET `attempts` = ? , `body` = ? , `dedupe_key` = ? , `extra_data` = ? , `is_read` = ? , `last_error` = ? , `method` = ? , `provider_message_id` = ? , `provider_status` = ? , `sender_alias` = ? , `sent_at` = ? , STATUS = ? , `target_identifier` = ? , `title` = ? , TYPE = ? , `updated_at` = ? WHERE `id` = ?	1196	1025.15	857.15	1196
UPDATE `notification` `nje1_0` SET STATUS = ? WHERE `nje1_0` . `id` = ? AND `nje1_0` . `status` IN (...)	1193	895.64	750.74	1196
INSERT INTO `notification` ( `attempts` , `body` , `created_at` , `dedupe_key` , `extra_data` , `is_read` , `last_error` , `method` , `provider_message_id` , `provider_status` , `sender_alias` , `sent_at` , STATUS , `target_identifier` , `title` , TYPE , `updated_at` ) VALUES (...)	1195	815.02	682.03	0
UPDATE `event_publication` `djep1_0` SET STATUS = ? WHERE `djep1_0` . `id` = ? AND `djep1_0` . `status` != ?	1293	777.22	601.09	1293
DELETE `djep1_0` FROM `event_publication` `djep1_0` WHERE `djep1_0` . `id` = ?	1168	586.52	502.16	1169
```

## Index catalog

```text
TABLE_NAME	INDEX_NAME	COLUMN_NAME	SEQ_IN_INDEX
event_publication	event_publication_by_completion_date_idx	completion_date	1
event_publication	PRIMARY	id	1
fcm_token	PRIMARY	id	1
fcm_token	uk_fcm_token_owner_id	owner_id	1
friend_relations	idx_friend_relations_expired_at	expired_at	1
friend_relations	idx_friend_relations_high	high_user_id	1
friend_relations	idx_friend_relations_high	status	2
friend_relations	idx_friend_relations_low	low_user_id	1
friend_relations	idx_friend_relations_low	status	2
friend_relations	PRIMARY	friend_relation_id	1
friend_relations	uk_friend_pair	low_user_id	1
friend_relations	uk_friend_pair	high_user_id	2
notification	idx_notification_inbox	target_identifier	1
notification	idx_notification_inbox	method	2
notification	idx_notification_inbox	status	3
notification	idx_notification_inbox	created_at	4
notification	idx_notification_provider_message_id	provider_message_id	1
notification	idx_notification_status	status	1
notification	idx_notification_status	created_at	2
notification	PRIMARY	id	1
notification	uk_notification_dedupe_key	dedupe_key	1
terms	PRIMARY	id	1
terms	uk_terms_type_version	type	1
terms	uk_terms_type_version	version	2
user_agreement	fk_user_agreement_terms	terms_version_id	1
user_agreement	idx_user_agreement_history	user_id	1
user_agreement	idx_user_agreement_history	terms_version_id	2
user_agreement	idx_user_agreement_history	occurred_at	3
user_agreement	PRIMARY	id	1
users	idx_users_nickname	nickname	1
users	PRIMARY	id	1
users	uk_users_email	email	1
users	uk_users_provider_oidc_subject	provider	1
users	uk_users_provider_oidc_subject	oidc_subject	2
```

## Representative execution plans

### User lookup

```json
EXPLAIN
{
  "query_block": {
    "select_id": 1,
    "message": "no matching row in const table"
  }
}
```

### Friend relation lookup

```json
EXPLAIN
{
  "query_block": {
    "select_id": 1,
    "cost_info": {
      "query_cost": "2.06"
    },
    "ordering_operation": {
      "using_filesort": true,
      "cost_info": {
        "sort_cost": "0.40"
      },
      "table": {
        "table_name": "friend_relations",
        "access_type": "index_merge",
        "possible_keys": [
          "uk_friend_pair",
          "idx_friend_relations_low",
          "idx_friend_relations_high"
        ],
        "key": "sort_union(uk_friend_pair,idx_friend_relations_high)",
        "key_length": "16,16",
        "rows_examined_per_scan": 2,
        "rows_produced_per_join": 0,
        "filtered": "20.00",
        "cost_info": {
          "read_cost": "1.62",
          "eval_cost": "0.04",
          "prefix_cost": "1.66",
          "data_read_per_join": "480"
        },
        "used_columns": [
          "friend_relation_id",
          "low_user_id",
          "high_user_id",
          "status",
          "initiated_user_id",
          "message",
          "low_alias",
          "high_alias",
          "expired_at",
          "created_at",
          "updated_at"
        ],
        "attached_condition": "(((`rati`.`friend_relations`.`low_user_id` = <cache>(uuid_to_bin('00000000-0000-0000-0000-000000000001'))) or (`rati`.`friend_relations`.`high_user_id` = <cache>(uuid_to_bin('00000000-0000-0000-0000-000000000001')))) and (`rati`.`friend_relations`.`status` = 'ACCEPTED'))"
      }
    }
  }
}
```

### Notification inbox

```json
EXPLAIN
{
  "query_block": {
    "select_id": 1,
    "cost_info": {
      "query_cost": "0.35"
    },
    "ordering_operation": {
      "using_filesort": false,
      "table": {
        "table_name": "notification",
        "access_type": "ref",
        "possible_keys": [
          "idx_notification_inbox",
          "idx_notification_status"
        ],
        "key": "idx_notification_inbox",
        "used_key_parts": [
          "target_identifier",
          "method",
          "status"
        ],
        "key_length": "1024",
        "ref": [
          "const",
          "const",
          "const"
        ],
        "rows_examined_per_scan": 1,
        "rows_produced_per_join": 1,
        "filtered": "100.00",
        "backward_index_scan": true,
        "cost_info": {
          "read_cost": "0.25",
          "eval_cost": "0.10",
          "prefix_cost": "0.35",
          "data_read_per_join": "16K"
        },
        "used_columns": [
          "id",
          "dedupe_key",
          "target_identifier",
          "method",
          "sender_alias",
          "type",
          "title",
          "body",
          "extra_data",
          "status",
          "attempts",
          "last_error",
          "sent_at",
          "provider_message_id",
          "provider_status",
          "is_read",
          "created_at",
          "updated_at"
        ]
      }
    }
  }
}
```

## EXPLAIN ANALYZE timings

### User lookup

```text
EXPLAIN
-> Zero rows (no matching row in const table)  (cost=0..0 rows=0) (actual time=76e-6..76e-6 rows=0 loops=1)
```

### Friend relation lookup

```text
EXPLAIN
-> Limit: 21 row(s)  (cost=1.66 rows=2) (actual time=0.0497..0.0497 rows=0 loops=1)
    -> Sort: friend_relations.friend_relation_id, limit input to 21 row(s) per chunk  (cost=1.66 rows=2) (actual time=0.0492..0.0492 rows=0 loops=1)
        -> Filter: (((friend_relations.low_user_id = <cache>(uuid_to_bin('00000000-0000-0000-0000-000000000001'))) or (friend_relations.high_user_id = <cache>(uuid_to_bin('00000000-0000-0000-0000-000000000001')))) and (friend_relations.`status` = 'ACCEPTED'))  (cost=1.66 rows=2) (actual time=0.0434..0.0434 rows=0 loops=1)
            -> Sort-deduplicate by row ID  (cost=1.66 rows=2) (actual time=0.0427..0.0427 rows=0 loops=1)
                -> Index range scan on friend_relations using uk_friend_pair over (low_user_id = 0x00000000000000000000000000000001)  (cost=0.36 rows=1) (actual time=0.0185..0.0185 rows=0 loops=1)
                -> Index range scan on friend_relations using idx_friend_relations_high over (high_user_id = 0x00000000000000000000000000000001)  (cost=0.36 rows=1) (actual time=0.0151..0.0151 rows=0 loops=1)
```

### Notification inbox

```text
EXPLAIN
-> Limit: 21 row(s)  (cost=0.35 rows=1) (actual time=0.0184..0.0184 rows=0 loops=1)
    -> Index lookup on notification using idx_notification_inbox (target_identifier='00000000-0000-0000-0000-000000000001', method='FCM', status='SENT') (reverse)  (cost=0.35 rows=1) (actual time=0.018..0.018 rows=0 loops=1)
```

## AI optimizer action

The AI must select candidates from the digest and execution-plan evidence, apply one attributable change per candidate, rerun the identical workload, and record accepted/rejected reasoning in the parent experiment report.
