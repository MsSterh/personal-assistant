# Phase 2 — Validation

## Automated Tests

All tests run via `clj -M:test`. Phase is not done until every item passes with no failures or warnings from clj-kondo.

| Test | What it proves |
|---|---|
| `load-user-profile` with file present | File content returned correctly |
| `load-user-profile` with file absent | Returns `""`, no exception |
| `load-memory` with file present | File content returned correctly |
| `load-memory` with file absent | Returns `""`, no exception |
| `build-memory-context` | Output contains both profile and memory sections with headers |
| `extract-new-facts` — zero markers | Returns empty seq |
| `extract-new-facts` — one marker | Returns seq with one fact string |
| `extract-new-facts` — multiple markers | Returns all fact strings |
| `strip-markers` | Markers removed; surrounding text unchanged |
| `append-facts-to-daily-note` | Creates file if absent; appends facts as list items |
| Existing Phase 1 REPL-loop tests | No regressions after system prompt change |

## Manual Checks

1. **User context visible** — start the assistant with a `USER.md` containing a known fact (e.g., "Name: Ada"). Ask "what's my name?" — the assistant should answer correctly from context.
2. **Memory context visible** — populate `MEMORY.md` with a fact. Ask a question that requires it. Assistant answers correctly.
3. **Daily note created** — send a message that causes the assistant to emit a memory marker. Verify `memory/YYYY-MM-DD.md` exists and contains the extracted fact.
4. **Marker stripped** — the marker does not appear in the terminal output shown to the user.
5. **Missing files non-fatal** — remove `USER.md` and `MEMORY.md`; assistant starts and converses normally.

## Definition of Done

- [ ] All automated tests in the table above pass.
- [ ] clj-kondo reports zero errors or warnings on `src/` and `test/`.
- [ ] All five manual checks completed successfully.
- [ ] No regressions in Phase 0 or Phase 1 functionality.
- [ ] Branch merged to `master` (PR or direct merge).
