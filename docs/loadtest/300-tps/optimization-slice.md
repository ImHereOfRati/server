# 300 TPS optimization — remove inbox count query

The SQL snapshot showed a separate `COUNT(*)` query for every notification inbox request, examining roughly 31 rows per call and consuming a large share of statement time. The inbox endpoint only needs the current page, so its Spring Data repository method now returns `Slice` instead of `Page`. This removes the count query while preserving page contents and ordering.

This change does not increase the connection pool or database connection limit.
