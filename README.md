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
  around the event-publish call).
- `GET /actuator/health`, `GET /actuator/prometheus`
- Swagger UI: `/swagger-ui/index.html`

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
docker run --network host sun-moon-order
```

`--network host` (Linux) so the container reaches Postgres at
`localhost:5432` on the host without extra networking setup — see the
[Debian-Setting `docs/docker.md`](https://github.com/schware/Debian-Setting/blob/master/docs/docker.md)
for how this is actually run (docker-compose, alongside KDS/Delivery,
memory limits).

Listens on port **8080** (see `application.yml`).
