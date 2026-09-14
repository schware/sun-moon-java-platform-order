# sun-moon-java-platform-order

The **Order** service — part of the `sun-moon-java-platform` family
(alongside [KDS](https://github.com/schware/sun-moon-java-platform-kds)
and [Delivery](https://github.com/schware/sun-moon-java-platform-delivery),
tied together by the
[umbrella repo](https://github.com/schware/sun-moon-java-platform) via
git submodules).

**Docker-deployed**, one container per service. Replaces
[sun-moon-java-platform-order-jetty](https://github.com/schware/sun-moon-java-platform-order-jetty)
(archived) — that version deployed as a WAR to one shared external Jetty
instance, which needed several workarounds only necessary *because* of
sharing one Jetty process (see that repo's ADR 0005). None of that applies
once each service owns its own container.

## Why Postgres + JSONB

Originally planned as MongoDB; switched because MongoDB 5.0+ requires AVX
and this homelab CPU doesn't have it. See the umbrella repo's
`docs/adr/0001`.

## API

- `POST /orders` — create an order (Jakarta Bean Validation + Resilience4j
  around the event-publish call). **409 if the store has not 개점'd.**
- `GET /orders` (`?status=`), `GET /orders/{id}`
- `PUT /orders/{id}/status` — the only way an order changes state
- `POST /business-days/open`, `POST /business-days/close` — 개점 / 마감
- `GET /business-days` — every store currently trading
- `GET /business-days/{storeId}` — that store's state, including
  `needsClosing`
- `GET /sales` (`?from=&to=&storeId=`) — 일자별 매출 집계
- `GET /sales/orders?businessDate=&storeId=` — the orders behind one day
- `GET /actuator/health`, `GET /actuator/prometheus`
- Swagger UI: `/swagger-ui/index.html`

## 영업일

Every order is stamped with the **영업일자** that was open when it
arrived — not the calendar date of `placedAt`, because a shop trading
past midnight is still on the same business day. A store that has not
개점'd cannot take orders at all.

Nothing closes a store on a timer. 마감 closes it; pressing 개점 on a day
that has already gone stale closes that one first and says so
(`rolled: true`). Until someone acts, `needsClosing` is how the POS knows
to prompt. 영업일자 is computed in `Asia/Seoul`, not in the host's UTC.

The full reasoning, including why this lives here rather than in BO
(which owns 매장 기준 정보), is the umbrella repo's `docs/adr/0006`.

## 매출

`GET /sales` groups by 영업일자 × 매장 and is what BO's 매출 조회 screen
reads. Postgres does the counting — `business_date` and `store_id` have
been real indexed columns since V4/V5 exactly so this can group on them,
and only `amount` comes out of the JSONB.

**금액은 수락된 주문만 셉니다.** `REJECTED` (가게가 거절했다) and
`EXPIRED` (가게가 아무 말도 안 했다) are orders that happened but not
sales that happened, and someone reading a day's total needs those to be
different numbers. They come back as their own counts, so a store that
refused half of what arrived can still see that.

Orders placed before 영업일 existed carry no `business_date` and are left
out — they predate the sales model rather than belonging to an unknown
day. Dates default to the last fortnight on the shop's Asia/Seoul
calendar, not the host's UTC one.

This is **not** the `Tr_header/Detail/Pay/Etc` structure the 설계서 calls
for. That needs the 거래번호 nothing mints yet; this is a read model over
the orders that exist today, and it keeps working once those tables
arrive.

## Real-time push to terminals

`EVENTS_REDIS=true` is what actually turns this on — the default
(`sun-moon.events.redis: false`) publishes events to a log line and
nowhere else, which is the right fallback for a machine with only a JDK
and no Redis, but is easy to leave on by accident in production: nothing
fails loudly, every terminal screen still works off its own ~15s poll,
and "real-time" quietly becomes "up to 15 seconds late." That is exactly
what happened on this project's own deploy server for a stretch —
`docker-start-all.sh` didn't set it, and every redeploy that copied its
`docker run` line carried the gap forward. Confirm it is on by tailing
this service's logs after a deploy: `RedisEventPublisher`, not
`InMemoryEventPublisher`, should be doing the logging.

## Build & run

Requires JDK 21+, and PostgreSQL reachable at `localhost:5432` with an
`order_service` database and `sunmoon` role.

```
./gradlew test
./gradlew bootJar
java -jar build/libs/sun-moon-java-platform-order-0.1.0.jar
```

## Docker

```
docker build -t sun-moon-order .
docker run --network host -e EVENTS_REDIS=true sun-moon-order
```

`--network host` (Linux) so the container reaches Postgres at
`localhost:5432` on the host without extra networking setup — see the
[Debian-Setting `docs/docker.md`](https://github.com/schware/Debian-Setting/blob/master/docs/docker.md)
for how this is actually run (docker-compose, alongside KDS/Delivery,
memory limits).

Listens on port **8080** (see `application.yml`).
