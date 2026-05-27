# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build executable JAR
mvn clean package

# Run application
mvn spring-boot:run
# or
java -jar target/Autoposting-bot-0.0.1-SNAPSHOT.jar

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Skip tests during build
mvn clean package -DskipTests
```

## Tech Stack

- **Java 20**, Spring Boot 3.4.3
- **Telegram Bots API 6.9.7.1** (`TelegramLongPollingCommandBot`)
- **PostgreSQL** via Spring Data JPA (Hibernate, `ddl-auto=update`)
- **Yandex Cloud S3** via AWS SDK v2 with custom endpoint
- **Replicate API** for Google Gemini image generation
- **Lombok** for boilerplate reduction

## Architecture

### State-Machine Handler Chain

The bot uses a **state machine** pattern where each user's progress is tracked in a `Map<Long, States>` (keyed by Telegram user ID). The `States` enum drives handler selection.

**Core dispatch flow:**
1. `MyBotForPosting.processNonCommandUpdate()` receives every Telegram update
2. `ClassifiedUpdate` wraps it, parsing: user ID, message type (TEXT, CALLBACK, PHOTO, etc.), text/data
3. `HandlerUtils.getHandler()` iterates handlers and returns the first whose `condition()` matches
4. Handler's `proceed()` mutates state, `getAnswer()` returns `List<SendMessage>` queued to `messageStack`
5. `@Scheduled` task drains `messageStack` and sends messages; a separate task calls `PostUtils.sendPost()` every 30s to deliver scheduled posts to channels

### Handler Implementations

Each of the 13 handlers in `handler/` implements the `Handler` interface with two methods:
- `condition(ClassifiedUpdate, States)` → boolean — when this handler applies
- `proceed(ClassifiedUpdate)` → void — mutates state/DB
- `getAnswer(ClassifiedUpdate)` → `List<SendMessage>` — builds reply

Handlers are matched top-to-bottom in `HandlerUtils`. Order matters.

### Post Creation Workflow

Multi-step flow driven by `States` enum transitions:
1. **ADDPOST** callback → channel selection
2. **CHANNEL** callback → creates in-memory `Post`, opens calendar (date selection)
3. **POSTDATE** → sets `Post.date`, shows hour options
4. **HOUR** → shows 15-min-interval time slots (filtered for already-taken slots)
5. **POSTTIME** → sets `Post.time`, transitions to `INPUT_FIRSTPROMPT`
6. **Text in `INPUT_FIRSTPROMPT`** → stores prompt, transitions to `FIRST_MEDIA`
7. **Photo in `FIRST_MEDIA`** → triggers async `PromptGenerator.createPost()`:
   - Downloads image to temp file
   - Calls Gemini via Replicate API → generates image title/tags
   - Uploads image to Yandex S3
   - Persists `Post` to PostgreSQL

### Key Classes

| Class | Location | Role |
|---|---|---|
| `MyBotForPosting` | root package | Main bot, long-polling, dispatch |
| `ClassifiedUpdate` | `utils/` | Update wrapper with parsed metadata |
| `HandlerUtils` | `utils/` | Selects handler from registered list |
| `States` | root | Enum of all user workflow states |
| `PromptGenerator` | `utils/` | Async: Gemini API → S3 upload → DB save |
| `PostUtils` | `utils/` | In-memory post staging + scheduled delivery |
| `S3Utils` | `utils/` | Yandex Cloud S3 upload |
| `MenuGenerator` | `utils/` | Inline keyboard / reply keyboard builders |
| `CalendarUtil` | `utils/` | Interactive date-picker keyboard |
| `FilesUtils` | `utils/` | Temp file management with TTL cleanup |
| `BotConfig` | `config/` | Bot registration, S3Client bean, `@EnableAsync` |

### Data Model

Three JPA entities in `model/`:
- **`Post`** — core entity: `channelId`, `date`, `time`, `title`, `tags`, `firstPrompt`, `firstMedia` (S3 URL), `sent`
- **`MainTag`** — reusable tag groups with a title
- **`OtherTag`** — individual tag values

### Access Control

Only Telegram user IDs `2025969289` and `697003177` are authorized. This check is enforced in `MyBotForPosting` before any handler runs.

## Configuration

All secrets live in `src/main/resources/application.properties`:
- `bot.token` — Telegram bot token
- `spring.datasource.*` — PostgreSQL at `45.8.157.109:5432/posting`
- `replicate.key` — Replicate API key (Gemini access)
- `s3.key`, `s3.secret`, `s3.region`, `s3.endpoint`, `s3.bucket` — Yandex Cloud S3
