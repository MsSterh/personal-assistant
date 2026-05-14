# Plan — Phase 0: Project Skeleton

## Group 1 — deps.edn and dependency declaration

1. Create `deps.edn` at the repo root with `:deps` map containing hato, cheshire, aero, and org.clojure/test.check / clojure.test (bundled with Clojure).
2. Add `:aliases` for test runner (`:test`) and linting (`:lint`, `:fmt`).
3. Verify deps resolve: `clj -P`.

## Group 2 — Directory scaffold

4. Create `src/` (placeholder namespace `personal_assistant/core.clj` with a no-op `main`).
5. Create `test/` (placeholder `personal_assistant/core_test.clj`).
6. Confirm `spec/` already exists; create `resources/` directory.

## Group 3 — Configuration files

7. Write `resources/config.edn.example` with placeholder keys: `:api-base-url`, `:model`, `:api-key`.
8. Add `resources/config.edn` to `.gitignore` (do not commit real credentials).
9. Document the config loading approach (aero + env var overrides) in a code comment in core.clj.

## Group 4 — Linting and formatting

10. Add `.clj-kondo/config.edn` with project-level config (namespace lint rules).
11. Add `.cljfmt.edn` with formatting rules (indentation, blank lines).
12. Run `clj -M:lint` and `clj -M:fmt --check`; fix any issues.

## Group 5 — Hello-world smoke test

13. Implement `personal-assistant.core/greet` to print a greeting and return `:ok`.
14. Write `personal-assistant.core-test/test-greet` asserting return value is `:ok`.
15. Run `clj -T:test`; confirm green.
16. Manually run `clj -M -m personal-assistant.core` and verify greeting prints to stdout.
