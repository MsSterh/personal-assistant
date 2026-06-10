# Roadmap

Each phase is a shippable increment. A phase is done when its tests pass and the feature works end-to-end in the console.

---

## Phase 0 — Project skeleton

- `deps.edn` with hato, cheshire, aero, clojure.test.
- Directory layout: `src/`, `test/`, `spec/`, `resources/`.
- `config.edn` template (git-ignored); `config.edn.example` committed.
- clj-kondo and cljfmt configured.
- "hello world" smoke test proves the build works.

## Phase 1 — Console REPL loop + LLM call

- Read-eval-print loop: user types a message, assistant replies, repeat.
- `SOUL.md` loaded at startup and injected as the system prompt.
- OpenAI-compatible POST to `/chat/completions`; response printed to stdout.
- HTTP client hidden behind a protocol; fake client used in tests.
- Tests: message round-trip with fake client, SOUL.md loading.

## Phase 2 — User identity & persistent memory

- `USER.md` loaded at startup and appended to system context.
- `MEMORY.md` read at startup; relevant facts included in each request.
- `memory/YYYY-MM-DD.md` daily note: assistant writes new facts it decides to keep.
- Tests: memory read/write, context injection.

## Phase 3 — Safe file tools

- Assistant can read any local file (path passed by user).
- Assistant can write/edit files only inside the current working directory.
- Path validation rejects traversal attempts (`../`, absolute paths outside CWD).
- Tests: allowed reads, blocked writes, traversal rejection.

## Phase 4 — Web tools

- Web search: query → list of result snippets (via a configurable search API).
- Web page reader: URL → extracted plain text (HTML stripped).
- Both tools reuse the LLM function-call registry & dispatch layer built in Phase 3; assistant decides when to invoke them.
- Tests: fake HTTP responses, tool dispatch logic.

## Phase 5 — Heartbeat & cron jobs

- `HEARTBEAT.md` defines a checklist of recurring jobs (e.g., clean stale memories).
- Dedicated CLI entry point (`-main` in `heartbeat` ns) processes the checklist.
- Cron or launchd invokes it on a schedule the user configures.
- Assistant can add new tasks to `HEARTBEAT.md` during conversation.
- Tests: job parsing, execution, task creation.

## Phase 6 — Polish & hardening

- `AGENTS.md` and `TOOLS.md` loaded and respected (behavioral rules, infra hints).
- Graceful error handling: API failures, missing files, malformed config.
- Conversation history truncation to stay within context limits.
- End-to-end integration test covering a full session with fake LLM.
- README updated with setup and usage instructions.
