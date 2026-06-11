# Phase 3 — Safe File Tools: Plan

## 1. Path validation module

- [ ] Create `src/personal_assistant/paths.clj`.
- [ ] `cwd-root` — resolve the current working directory to a canonical
      absolute `java.io.File` / path once.
- [ ] `resolve-path` — turn a user-supplied path into a canonical absolute path
      (resolving `.`, `..`, and symlinks).
- [ ] `within-cwd?` — predicate: does a resolved path fall inside `cwd-root`?
- [ ] `validate-write-path` — return the resolved path or a structured
      `{:error ...}` for paths that escape CWD or attempt traversal.
- [ ] Tests in `test/personal_assistant/paths_test.clj`: traversal (`../`),
      absolute-outside-CWD, symlink escape, and valid in-CWD paths.

## 2. File tool functions

- [ ] Create `src/personal_assistant/tools/files.clj`.
- [ ] `read-file` — read any readable path; return contents or structured error.
- [ ] `write-file` — create/overwrite inside CWD (validates via `paths`).
- [ ] `edit-file` — exact string replacement inside CWD; error if the match is
      missing or non-unique.
- [ ] `delete-file` — remove a file inside CWD.
- [ ] `move-file` — move/rename inside CWD (validate both source and dest).
- [ ] `list-dir` — list/glob a directory's entries (read-anywhere).
- [ ] Each fn returns a plain map suitable for serialization back to the LLM.

## 3. Tool registry & dispatch (Phase 4 pattern, pulled forward)

- [ ] Create `src/personal_assistant/tools/registry.clj`.
- [ ] OpenAI-format function schema for each tool (name, description,
      parameters).
- [ ] `tool-defs` — vector of schemas to send with the chat request.
- [ ] `dispatch` — given a parsed `tool_call`, look up and invoke the tool fn,
      returning a `{:role "tool" :tool_call_id ... :content ...}` message.
- [ ] Tests in `test/personal_assistant/tools/registry_test.clj`: dispatch to
      each tool, unknown-tool handling, argument parsing.

## 4. LLM client tool-call support

- [ ] Extend `LLMClient/chat` (or add `chat-with-tools`) in
      `src/personal_assistant/llm.clj` to pass `:tools` and return any
      `tool_calls` on the assistant message.
- [ ] Update `HatoClient` to serialize tool defs and parse `tool_calls`.
- [ ] Update the test fake client (`test/personal_assistant/fake_client.clj`)
      to script tool-call responses followed by a final text reply.
- [ ] Tests in `test/personal_assistant/llm_test.clj` for the tool-call
      request/response round-trip.

## 5. REPL integration

- [ ] In `src/personal_assistant/repl.clj`, wrap the single `chat` call in a
      loop: while the reply contains `tool_calls`, dispatch each, append tool
      results to history, and call again until a plain text reply returns.
- [ ] Pass `registry/tool-defs` into the chat call.
- [ ] Preserve existing memory-marker extraction on the final text reply.
- [ ] Tests in `test/personal_assistant/repl_test.clj`: a scripted session
      where the assistant calls a file tool and then replies.

## 6. Lint, format, manual check

- [ ] `clj-kondo` clean across new namespaces.
- [ ] `cljfmt` clean.
- [ ] Manual REPL session: read an out-of-CWD file, write a file in CWD, and
      confirm a write outside CWD is refused.
