# Phase 2 — User Identity & Persistent Memory

## Overview

Give the assistant a persistent model of the user. At startup, load `USER.md` (static profile) and `MEMORY.md` (accumulated facts) and inject both into the system context so every conversation is personalised. After each exchange, the assistant may append new facts to a daily note file (`memory/YYYY-MM-DD.md`), which accumulates into `MEMORY.md` over time.

## In Scope

- Load `USER.md` from the working directory at startup; append its content to the system prompt.
- Load `MEMORY.md` from the working directory at startup; append all facts to the system prompt (no filtering this phase).
- After each assistant turn, parse the response for any new facts the assistant chose to record and write them to `memory/YYYY-MM-DD.md` (today's date), creating the file if it does not exist.
- A dedicated `personal-assistant.memory` namespace encapsulates all memory read/write/format logic.
- Unit tests for: loading USER.md, loading MEMORY.md, injecting into context, writing to daily note.

## Out of Scope

- Relevance filtering or semantic search over memory facts.
- Automatic summarisation or merging of daily notes into MEMORY.md (deferred to Phase 5/6).
- Embedding-based retrieval.
- Any changes to the HTTP client or LLM dispatch logic.

## Decisions & Constraints

- **File format:** Plain Markdown. `MEMORY.md` is a flat list of facts; `memory/YYYY-MM-DD.md` accumulates appended lines per session.
- **No database.** All I/O is plain `slurp` / `spit` via `clojure.java.io`, consistent with the project's minimal-footprint value.
- **Missing files are non-fatal.** If `USER.md` or `MEMORY.md` does not exist, startup continues with an empty context for that file (log a warning).
- **Memory write protocol:** The assistant signals facts to persist via a structured marker in its response (exact format TBD during implementation; a simple `<!-- remember: ... -->` comment or a JSON block are candidates). The memory namespace parses this and strips the marker from the displayed output.
- **Single user.** No multi-tenant concerns; file paths are relative to CWD.
