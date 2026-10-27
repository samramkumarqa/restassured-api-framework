# Rest Assured API Framework

[![API Tests](https://github.com/samramkumarqa/restassured-api-framework/actions/workflows/api-tests.yml/badge.svg)](https://github.com/samramkumarqa/restassured-api-framework/actions/workflows/api-tests.yml)

REST API test automation in **Java 17** with **Rest Assured** and **TestNG**, against the [Restful-Booker](https://restful-booker.herokuapp.com/apidoc/index.html) hotel booking API.

Two suites share one client layer:

- **Live:** full CRUD with token auth, search, content negotiation, JSON schema contracts and negative cases against the real API.
- **Mock:** the same `BookingClient` pointed at a local **WireMock** server to test what a live API can't produce on demand (503s, broken JSON, dropped connections, timeouts) and to verify the exact requests the client sends. Runs offline.

## Highlights

| Area | What it shows |
| --- | --- |
| Client layer | `AuthClient` and `BookingClient` own paths, auth and payloads; tests own the assertions |
| Shared spec | One `RequestSpecification`: base URI, JSON, timeouts, Allure filter |
| Typed models | Java records (`Booking`, `CreatedBooking`, …) serialised and deserialised by Jackson |
| Contract tests | JSON Schema validation for booking, created-booking and id-list responses |
| Full CRUD | POST → GET → PUT → PATCH → DELETE → 404, comparing whole objects |
| Auth | Token requested once per run and sent as the `token` cookie; negative cases for missing auth |
| Data-driven | Rows from `testdata/bookings.json` and TestNG data providers |
| Negative cases | 404, 403, 400, 405, 418 and the API's 500-on-missing-field behaviour, pinned explicitly |
| Mocking | WireMock: request verification, server errors, schema breaks, malformed JSON, connection reset, read timeout |
| Self-cleaning | Every booking a test creates is deleted afterwards, even when the test fails |
| Reporting | Allure, with each request and response attached |
| CI | GitHub Actions runs mock and live suites in parallel on every push, plus a weekday run to catch API changes |

## API quirks the tests document

Restful-Booker behaves differently from a typical REST API, and the tests assert its real behaviour:

| Behaviour | Status |
| --- | --- |
| Health check `/ping` | **201** |
| Bad login credentials | **200** with `{"reason": "Bad credentials"}` |
| Create with a missing required field | **500** |
| Update or delete a booking that doesn't exist | **405** |
| Successful delete | **201** |
| `Accept` header other than an exact supported value | **418** |
| Booking requested as XML | XML body, but `Content-Type: text/html` |

The last one matters for Rest Assured: `ContentType.JSON` sends several types in `Accept`, which this API rejects, so the shared spec sets `Accept: application/json` literally.

## Project structure

```
src/main/java/com/qa/api/
├── config/    Config (properties → env vars → -D flags)
├── spec/      Specs: shared RequestSpecification
├── client/    AuthClient (token cache), BookingClient
├── models/    Booking, BookingDates, CreatedBooking, AuthResponse, …
└── data/      BookingFactory (Datafaker)
src/test/java/com/qa/tests/
├── base/      BaseApiTest (clients, cleanup)
├── live/      Health, Auth, CRUD, data-driven, negative, XML
└── mock/      BookingClientMockTest (WireMock)
src/test/resources/
├── schemas/   JSON Schemas
├── testdata/  bookings.json
└── suites/    regression, smoke, live, mock
```

## Running

Requires Java 17+ and Maven.

```bash
mvn test                    # everything (live + mock)
mvn test -Dsuite=smoke      # smoke group
mvn test -Dsuite=live       # live API only
mvn test -Dsuite=mock       # WireMock only, no network needed
mvn allure:serve            # open the Allure report
```

To test a local copy of the API instead of the shared one:

```bash
docker run -d -p 3001:3001 mwinteringham/restfulbooker
mvn test -Dbase.url=http://localhost:3001
```

## Configuration

| Setting | Default | Override |
| --- | --- | --- |
| `base.url` | `https://restful-booker.herokuapp.com` | `-Dbase.url=…` or `BASE_URL` |
| `api.username` / `api.password` | Restful-Booker's public demo login | `API_USERNAME` / `API_PASSWORD` |
| Timeouts | 10 s connect, 30 s read | `connect.timeout.ms`, `read.timeout.ms` |
