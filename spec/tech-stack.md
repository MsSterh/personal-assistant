# Tech Stack

## Language & runtime

- **Clojure** (JVM) — primary language for all application logic and tests.
- `deps.edn` (tools.deps) — dependency management and CLI entry points.

## LLM integration

- **OpenAI-compatible REST API** — the assistant calls any endpoint that speaks the OpenAI chat completions format.
- Provider is configured via environment variable or a local config file (e.g., `config.edn`). Switching between Anthropic, OpenAI, or a local Ollama instance requires only a config change, not code changes.
- HTTP client: **hato** (wraps Java 11 `HttpClient`; no native deps). JSON: **cheshire**.

## Configuration

- `config.edn` (git-ignored) holds the API base URL, model name, and API key.
- **aero** for config loading with environment variable overrides.

## File I/O

Plain Clojure `clojure.java.io` / `slurp` / `spit`. No database.

## Scheduling

External cron (system crontab or launchd) invokes a dedicated entry point for heartbeat jobs. No in-process scheduler.

## Testing

- **clojure.test** — unit and integration tests.
- All public functions must have tests before a feature is considered done.
- HTTP calls are wrapped behind a protocol so tests can substitute a fake client without hitting the network.

## Linting & formatting

- **clj-kondo** for static analysis.
- **cljfmt** for formatting.
