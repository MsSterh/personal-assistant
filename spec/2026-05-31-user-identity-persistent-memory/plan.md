# Phase 2 — Plan

## 1. Memory namespace scaffold

- [ ] Create `src/personal_assistant/memory.clj`.
- [ ] Define public API: `load-user-profile`, `load-memory`, `build-memory-context`, `extract-new-facts`, `append-facts-to-daily-note`.
- [ ] Add namespace to `deps.edn` source paths (already covered by `src/`).

## 2. File loading

- [ ] `load-user-profile [path]` — slurp `USER.md`; return empty string if file absent (log warning).
- [ ] `load-memory [path]` — slurp `MEMORY.md`; return empty string if file absent (log warning).
- [ ] `build-memory-context [profile memory]` — concatenate into a system-context string with clear section headers.

## 3. System prompt injection

- [ ] In `core.clj` (or wherever the system prompt is assembled), call `memory/build-memory-context` at startup.
- [ ] Prepend (or append) the result to the existing SOUL.md system prompt — keep SOUL first, user context second.

## 4. Memory write: fact extraction

- [ ] Decide and document the marker format (e.g., `<!-- remember: <fact> -->`).
- [ ] `extract-new-facts [assistant-response]` — parse all markers from the response string; return a seq of fact strings.
- [ ] `strip-markers [assistant-response]` — remove markers from the string before displaying to the user.

## 5. Daily note write

- [ ] `append-facts-to-daily-note [facts date-str]` — create `memory/` dir if missing; open `memory/YYYY-MM-DD.md`; append each fact as a list item.
- [ ] Wire into the REPL loop: after each assistant reply, call `extract-new-facts` → `append-facts-to-daily-note`, then display stripped response.

## 6. Tests

- [ ] `test/personal_assistant/memory_test.clj` — unit tests for all public functions:
  - [ ] `load-user-profile` with existing file.
  - [ ] `load-user-profile` with missing file (returns `""`).
  - [ ] `load-memory` with existing file.
  - [ ] `load-memory` with missing file (returns `""`).
  - [ ] `build-memory-context` formats correctly.
  - [ ] `extract-new-facts` parses zero, one, and multiple markers.
  - [ ] `strip-markers` removes all markers leaving clean text.
  - [ ] `append-facts-to-daily-note` writes expected lines to a temp file.
- [ ] Integration: existing REPL-loop tests still pass after context injection change.

## 7. Manual smoke test

- [ ] Create sample `USER.md` and `MEMORY.md` in project root.
- [ ] Run a console session; verify the assistant references user context.
- [ ] Send a message that causes the assistant to emit a memory marker; verify the daily note is created and populated.
- [ ] Verify the marker is stripped from displayed output.
