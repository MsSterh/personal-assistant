# Phase 3 — Safe File Tools: Requirements

## Overview

Give the assistant the ability to interact with the local filesystem through a
small set of safe, LLM-invokable tools. Reads may target any local path; all
mutating operations (write, edit, delete, move) are confined to the current
working directory. A path-validation layer rejects traversal and out-of-CWD
escapes before any operation runs.

This phase also pulls the LLM **function-calling / tool-dispatch** mechanism
forward from Phase 4, since the file tools are the first concrete consumers of
it. The web tools in Phase 4 will reuse the same registry and dispatch loop.

## In Scope

- **Read tool** — read the contents of any readable local file (path passed by
  the user / chosen by the assistant). Not restricted to CWD.
- **Write tool** — create or overwrite a file, restricted to inside CWD.
- **Edit tool** — exact string replacement within an existing file, restricted
  to inside CWD.
- **Delete tool** — remove a file, restricted to inside CWD.
- **Move/rename tool** — move or rename a file; both source and destination
  restricted to inside CWD.
- **List-directory tool** — list/glob entries of a directory so the assistant
  can discover files rather than guessing paths.
- **Path-validation module** — a standalone namespace that resolves and
  validates paths, rejecting `../` traversal, symlink escapes, and absolute
  paths outside CWD for mutating ops.
- **Tool registry + dispatch** — tools exposed to the LLM as OpenAI-style
  function definitions; a dispatch layer parses `tool_calls` from the response,
  executes the matching tool, feeds results back, and loops until the assistant
  produces a final text reply.
- **REPL integration** — the existing console loop drives the tool-call loop so
  the assistant can actually read/write files during a session.

## Out of Scope

- Web tools (Phase 4) — only the shared dispatch machinery is built here.
- Network access of any kind.
- Recursive directory operations (delete/move of whole trees).
- Permission prompts / interactive confirmation UI — safety is enforced purely
  by path validation.
- Binary-aware editing or diff application beyond exact string replacement.

## Decisions & Constraints

- **Read anywhere, mutate CWD-only.** Reads and directory listing accept any
  path; write/edit/delete/move are containment-checked against CWD.
- **CWD is resolved once** at process start to a canonical absolute path;
  containment is checked against the canonical real path of the target
  (resolving symlinks) so symlink escapes are caught.
- **Tools are plain Clojure fns** behind a registry map, each with an
  accompanying OpenAI-format schema. No reflection / dynamic eval.
- **Validation failures return a structured error** to the LLM (as the tool
  result), not an exception that crashes the loop — the assistant can recover.
- **`LLMClient/chat` is extended** to pass tool definitions and surface
  `tool_calls`; the `HatoClient` and the test fake client both implement it.
- Stack stays within Phase 0 deps (hato, cheshire, aero, clojure.test) — no new
  libraries. File I/O via `clojure.java.io` / `slurp` / `spit`.
- Aligns with mission's privacy-first, minimal-footprint values: plain files,
  no daemon, every tool covered by tests before done.
