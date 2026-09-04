# backend

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).


## Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|
| [AsyncAPI](https://start.ktor.io/p/com.asyncapi/server-asyncapi) | Generates and serves AsyncAPI documentation |
| [Caching Headers](https://start.ktor.io/p/io.ktor/server-caching-headers) | Provides options for responding with standard cache-control headers |
| [Compression](https://start.ktor.io/p/io.ktor/server-compression) | Compresses responses using encoding algorithms like GZIP |
| [Simple Cache](https://start.ktor.io/p/com.ucasoft/server-simple-cache) | Provides API for cache management |
| [Simple Memory Cache](https://start.ktor.io/p/com.ucasoft/server-simple-memory-cache) | Provides memory cache for Simple Cache plugin |
| [Authentication](https://start.ktor.io/p/io.ktor/server-auth) | Provides extension point for handling the Authorization header |
| [Authentication JWT](https://start.ktor.io/p/io.ktor/server-auth-jwt) | Handles JSON Web Token (JWT) bearer authentication scheme |
| [Request Validation](https://start.ktor.io/p/io.ktor/server-request-validation) | Adds validation for incoming requests |
| [Static Content](https://start.ktor.io/p/io.ktor/server-static-content) | Serves static files from defined locations |
| [Call Logging](https://start.ktor.io/p/io.ktor/server-call-logging) | Logs client requests |
| [Call ID](https://start.ktor.io/p/io.ktor/server-callid) | Allows to identify a request/call. |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library |
| [Exposed](https://start.ktor.io/p/org.jetbrains/server-exposed) | Adds Exposed database to your application |


## Building & Running

SQLite is the default database. On first startup, the backend creates the parent directory, the `data/renkei.db` database file, and all required tables automatically. Set `DATABASE_URL` only when a different location or database is needed.

To build or run the project, use one of the following tasks:


| Task | Description |
|------|-------------|
| `./gradlew test`    | Run the tests     |
| `./gradlew build`   | Build the project |
| `./gradlew run`     | Run the server    |

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### Docker

Build the backend image:

```shell
docker build -t renkei-backend .
```

Run it with the local environment file and a persistent Docker volume for SQLite:

```shell
docker run --rm \
  --name renkei-backend \
  -p 8080:8080 \
  --env-file .env \
  -e DATABASE_URL=jdbc:sqlite:/app/data/renkei.db \
  -e JAVA_OPTS="-Xms128m -Xmx512m" \
  -v renkei-data:/app/data \
  renkei-backend
```

The Docker build compiles the fat JAR in a Java 21 builder stage, then copies it into a Java 21 JRE image. `JAVA_OPTS` is optional and can be used to pass JVM startup arguments. Secrets are supplied at runtime and `.env` is excluded from the build context.

Alternatively, build and start the backend with Docker Compose:

```shell
docker compose up -d --build
```

Follow logs or stop the service with:

```shell
docker compose logs -f backend
docker compose down
```

Set `RENKEI_PORT` to change the host port or `JAVA_OPTS` to override the default JVM arguments. The SQLite database is stored in the `renkei-data` named volume.

## Bark message delivery

The backend sends a Bark notification after a device posts a message. The delivery path is:

```text
message publisher -> POST /message/send -> subscription lookup
                  -> iOS subscriber -> Bark server -> Bark app
```

Messages whose JSON-encoded UTF-8 body is at most 2 KiB are delivered in full. Longer messages contain a short preview and a link to a one-time web page where the full message can be viewed and copied. The link expires after ten minutes by default. Its 256-bit token is placed in the URL fragment, so it is not sent with the initial page request, and only a SHA-256 hash is stored by the backend.

All Bark content is encrypted with AES-256-GCM before it leaves the backend. Bark notification IDs are derived from the RenKei message ID so a repeated delivery updates the same notification instead of creating duplicates.

The subscription relation and delivery target are independent: `/subscription/{publisher_device_id}` decides which publisher a device follows, while `/notification-target/bark` stores where that subscriber receives notifications.

### Bark configuration

Copy `.env.example` to `.env` and configure these values:

| Variable | Description |
|---|---|
| `BARK_ENABLED` | Enables Bark delivery when set to `true`. |
| `BARK_BASE_URL` | Bark server URL. Defaults to `https://api.day.app`; use an HTTPS URL for a self-hosted production server. |
| `BARK_PUBLIC_BASE_URL` | Public HTTPS origin of this backend, used by long-message links, for example `https://renkei.example.com`. |
| `BARK_TITLE` | Title shown on Bark notifications. |
| `BARK_GROUP` | Bark notification group. |
| `BARK_ENCRYPTION_KEY` | Exactly 32 printable ASCII characters. Configure the same value in Bark as AES256 / GCM / noPadding. |
| `BARK_TIMEOUT_MILLIS` | Per-request connect and response timeout. |
| `BARK_MESSAGE_LINK_TTL_SECONDS` | Lifetime of a one-time long-message link. Defaults to `600`. |

Generate a 32-character key, then keep it in the deployment secret store rather than source control:

```shell
openssl rand -base64 24
```

In the Bark app, open **Push Encryption**, select `AES256`, `GCM`, and `noPadding`, enter the same key, and enter any 12-character IV. RenKei generates a fresh IV for every push and sends it with the encrypted payload, so the saved IV is only a setup placeholder.

`BARK_PUBLIC_BASE_URL` and `BARK_BASE_URL` must use HTTPS in production. Plain HTTP is accepted only for loopback development URLs.

The Bark device key is a bearer secret. Do not put it in logs, URLs, or source control. The backend sends it in a JSON body over HTTPS. The message encryption key is loaded from the environment and is never sent to Bark.

### Register a Bark target

Install Bark on the receiving iPhone and copy its device key. Register the key for the authenticated iOS device:

```http
PUT /notification-target/bark
Authorization: Bearer <device-jwt>
Content-Type: application/json

{"deviceKey":"<bark-device-key>"}
```

The authenticated device must be registered with platform `IOS`. Remove its target with:

```http
DELETE /notification-target/bark
Authorization: Bearer <device-jwt>
```

### Subscribe and send

The publisher is identified by its device ID, while the authenticated JWT identifies the subscriber:

```http
POST /subscription/{publisher_device_id}
Authorization: Bearer <device-jwt>
```

The endpoint is idempotent. `DELETE` on the same path removes the subscription. When the publisher later calls `POST /message/send`, every subscribed iOS device with a Bark target receives either the encrypted full message or an encrypted preview with a one-time full-message link.

Bark transport failures and temporary HTTP or Bark response codes (`429` and `5xx`) are retried up to three times with exponential backoff. Permanent failures are logged without exposing the device key.
