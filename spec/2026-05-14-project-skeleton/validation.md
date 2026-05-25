# Validation — Phase 0: Project Skeleton

## Merge gate

All three checks must pass. Run them in this order:

```bash
# 1. Tests green
clj -X:test

# 2. No lint warnings
clj -M:lint

# 3. Greeting visible at runtime
clj -M -m personal-assistant.core
```

Expected output for (3): a greeting line printed to stdout, process exits 0.

## Checklist

- [ ] `clj -X:test` exits 0 with at least one test passing.
- [ ] `clj -M:lint` exits 0 with no warnings.
- [ ] `clj -M:fmt --check` exits 0 (no formatting violations).
- [ ] Running `clj -M -m personal-assistant.core` prints a greeting to stdout.
- [ ] `resources/config.edn` is absent from git (confirm with `git status`).
- [ ] `resources/config.edn.example` is committed with placeholder values only.
- [ ] `src/`, `test/`, `spec/`, `resources/` directories all exist in the repo.

## Out-of-scope signals (do not block merge on these)

- Application logic, LLM calls, or multi-turn conversation — those are Phase 1.
- Performance, latency, or memory usage of the skeleton.
