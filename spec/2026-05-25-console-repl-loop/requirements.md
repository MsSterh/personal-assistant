# Phase 1 — Console REPL Loop + LLM Call

## Overview

Wire up a read-eval-print loop that accepts user input, sends it to an OpenAI-compatible LLM endpoint, and prints the reply. The assistant's personality is set by `SOUL.md`, loaded once at startup and injected as the system prompt. HTTP logic sits behind a protocol so tests never hit the network.

## In Scope

- Console REPL: read a line from stdin, call the LLM, print the response, repeat until EOF or quit command.
- `SOUL.md` loaded at startup; its contents become the system prompt for every request.
- HTTP client protocol with a production implementation (hato) and a fake implementation for tests.
- OpenAI-compatible `POST /chat/completions` with blocking (non-streaming) response.
- Config loaded from `config.edn` via aero: `:api-url`, `:model`, `:api-key`.
- Conversation history accumulated in memory for the session (no persistence yet).
- clojure.test suite covering the round-trip and SOUL.md loading.

## Out of Scope

- Streaming token output.
- Persistent conversation history across sessions.
- User identity (`USER.md`) or memory (`MEMORY.md`) — those are Phase 2.
- File tools, web tools, or any function-calling.
- Any provider-specific API (all calls go through the OpenAI-compatible interface).

## Decisions & Constraints

- **HTTP behind a protocol** — the `LLMClient` protocol has a single `chat` method; hato and the fake client both implement it. This keeps tests network-free.
- **Blocking HTTP** — no streaming; `hato/post` returns the full response body before the REPL prints.
- **SOUL.md format** — plain UTF-8 text, loaded with `slurp`, injected verbatim as the `system` role message.
- **Quit command** — typing `quit` or `exit` (case-insensitive) ends the loop cleanly.
- **Config via aero** — `resources/config.edn` (git-ignored); `resources/config.edn.example` committed.
