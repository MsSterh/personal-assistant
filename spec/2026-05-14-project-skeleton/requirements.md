# Requirements — Phase 0: Project Skeleton

## Scope

Phase 0 delivers a working project scaffold with zero application logic. Its only job is to prove the build, test, lint, and format toolchain functions correctly so every subsequent phase starts from a known-good baseline.

## Decisions

### Dependencies (deps.edn)

| Dep | Purpose |
|-----|---------|
| `hato` | HTTP client (wraps Java 11 HttpClient; no native deps) |
| `cheshire` | JSON encode/decode |
| `aero` | Config loading with env-var overrides |
| `clojure.test` | Bundled with Clojure; no extra coord needed |

Versions are pinned in `deps.edn`; no ranges.

### Directory layout

```
personal-assistant/
  src/personal_assistant/   ← application namespaces
  test/personal_assistant/  ← mirrored test namespaces
  spec/                     ← design documents (this dir)
  resources/                ← config templates, static assets
  deps.edn
  .gitignore
  README.md
```

### Configuration convention

- `resources/config.edn` is git-ignored; never committed.
- `resources/config.edn.example` is committed with placeholder values.
- aero reads `config.edn` at startup and merges environment variable overrides (see tech-stack.md).
- Required keys: `:api-base-url`, `:model`, `:api-key`.

### Toolchain aliases

Defined in `deps.edn` under `:aliases`:

- `:test` — runs `clojure.test` via `cognitect.test-runner` or equivalent.
- `:lint` — runs clj-kondo.
- `:fmt` / `:fmt-fix` — runs cljfmt check / fix.

### Smoke test

A single `greet` function in `personal-assistant.core` prints a greeting and returns `:ok`. Its test asserts the return value. This is the minimum required to call the build "proven."

## Context

- Mission: local-first, single-user Clojure console app (see `spec/mission.md`).
- Language: Clojure on JVM, deps.edn tooling (see `spec/tech-stack.md`).
- No application logic is in scope for Phase 0; that begins in Phase 1.
