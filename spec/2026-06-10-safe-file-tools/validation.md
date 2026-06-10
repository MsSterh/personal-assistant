# Phase 3 — Safe File Tools: Validation

## Automated Tests

**Path validation (`paths_test.clj`)**
- [ ] `../` traversal out of CWD is rejected for mutating ops.
- [ ] Absolute path outside CWD is rejected for mutating ops.
- [ ] Symlink that points outside CWD is rejected (resolved real path checked).
- [ ] Valid in-CWD path (including nested) is accepted.

**File tools (`files` tests)**
- [ ] Allowed read of an out-of-CWD file returns its contents.
- [ ] Write inside CWD creates/overwrites the file.
- [ ] Write outside CWD is refused with a structured error (no file created).
- [ ] Edit replaces an exact match; missing/non-unique match returns an error.
- [ ] Delete and move succeed inside CWD and are refused outside CWD.
- [ ] `list-dir` returns expected entries.

**Registry & dispatch (`registry_test.clj`)**
- [ ] Dispatch routes each tool name to the correct fn and returns a
      `tool`-role result message.
- [ ] Unknown tool name yields a structured error, not an exception.
- [ ] Tool-call arguments are parsed correctly from JSON.

**LLM client (`llm_test.clj`)**
- [ ] Request includes the tool definitions when tools are passed.
- [ ] `tool_calls` on the response are surfaced on the returned message.

**REPL loop (`repl_test.clj`)**
- [ ] A scripted fake client that emits a tool call followed by a text reply
      drives one full dispatch cycle and prints the final reply.
- [ ] Memory-marker extraction still runs on the final reply.

## Manual Checks

- [ ] Start the console app and ask the assistant to read a file outside CWD —
      it succeeds.
- [ ] Ask it to write a file inside CWD — the file appears on disk.
- [ ] Ask it to write outside CWD (e.g. `/tmp/x` or `../x`) — it is refused and
      the loop continues without crashing.
- [ ] Ask it to list a directory and then read a discovered file end-to-end.

## Definition of Done

- [ ] All automated tests above pass (`clojure.test`).
- [ ] `clj-kondo` reports no warnings on new/changed namespaces.
- [ ] `cljfmt` reports no formatting diffs.
- [ ] Tools work end-to-end in a real console session (manual checks pass).
- [ ] No new dependencies beyond the Phase 0 set; no network access in file
      tools.
- [ ] Feature works end-to-end in the console (roadmap done-criterion).
