# Phase 1 — Validation

## Automated Tests

- [ ] `clj -M:test` exits 0 with all tests passing.
- [ ] Round-trip test: `FakeClient` receives the expected messages vector (system prompt + user turn) and the REPL prints its canned reply.
- [ ] SOUL.md loading test: `load-soul` returns `{:role "system" :content <soul-text>}` for a known temp file.
- [ ] REPL loop test: a single simulated stdin line produces the fake reply on stdout; loop exits cleanly on `quit`.
- [ ] `clj-kondo --lint src test` — zero warnings.
- [ ] `cljfmt check src test` — zero formatting violations.

## Manual Checks

- [ ] `cp resources/config.edn.example resources/config.edn`, fill in a real API key and model name.
- [ ] Run `clj -M:repl`, type a message, receive a coherent reply from the live LLM.
- [ ] Verify the system prompt (from `SOUL.md`) is reflected in the assistant's tone or self-description.
- [ ] Type a follow-up message; confirm the assistant references the previous turn (history is working).
- [ ] Type `quit`; confirm the process exits cleanly with no stack trace.

## Definition of Done

All automated tests pass, clj-kondo and cljfmt report no issues, and a manual smoke test against a live endpoint completes a two-turn conversation with visible personality from `SOUL.md`.
