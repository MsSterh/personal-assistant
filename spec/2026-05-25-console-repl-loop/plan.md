# Phase 1 — Plan

## 1. REPL loop

- [ ] Create `src/personal_assistant/repl.clj` with a `-main` entry point.
- [ ] Implement `run-loop` that reads stdin line by line, calls the LLM, and prints the reply.
- [ ] Handle quit commands (`quit`, `exit`) and EOF gracefully.
- [ ] Accumulate conversation history as a vector of `{:role :content}` maps in a local atom.

## 2. LLM protocol + HTTP client

- [ ] Define `LLMClient` protocol in `src/personal_assistant/llm.clj` with a single `chat [client messages]` method.
- [ ] Implement `HatoClient` record using hato to `POST /chat/completions`; parse JSON response with cheshire.
- [ ] Implement `FakeClient` record in `test/` that returns a configurable canned response.
- [ ] Add `:repl` alias in `deps.edn` pointing to `personal-assistant.repl/-main`.

## 3. SOUL.md loading

- [ ] Create `resources/SOUL.md` with a starter personality blurb.
- [ ] Add `load-soul` function in `src/personal_assistant/context.clj` that slurps `SOUL.md` and returns a system message map.
- [ ] Prepend the system message to every messages vector before sending to the LLM.

## 4. Config wiring

- [ ] Verify `resources/config.edn.example` has `:api-url`, `:model`, `:api-key` keys.
- [ ] Add `src/personal_assistant/config.clj` with `load-config` using aero; throws a clear error if `config.edn` is missing.
- [ ] Pass config into `HatoClient` constructor at startup.

## 5. Tests

- [ ] `test/personal_assistant/llm_test.clj` — round-trip test: `FakeClient` returns a known reply; assert it appears in conversation history.
- [ ] `test/personal_assistant/context_test.clj` — SOUL.md loading: create a temp file, call `load-soul`, assert role and content.
- [ ] `test/personal_assistant/repl_test.clj` — single-turn loop test with `FakeClient`: assert stdout contains the fake reply.
- [ ] All tests run with `clj -M:test`; clj-kondo and cljfmt pass with no warnings.
