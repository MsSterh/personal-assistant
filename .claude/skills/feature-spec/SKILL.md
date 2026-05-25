# Feature Spec Skill

Create a spec directory and starter documents for the next unstarted phase on the roadmap.

## Steps

1. **Find the next phase** — Read `spec/roadmap.md`. Identify the first phase that does not yet have a matching directory under `spec/` (directories are named `YYYY-MM-DD-<phase-slug>`). Read `spec/mission.md` and `spec/tech-stack.md` for project context.

2. **Ask the user** — Before writing anything to disk, call `AskUserQuestion` with all three questions grouped in a single call:
   - **Scope / decisions** — What is in scope and out of scope? Any key decisions already made?
   - **Tasks** — What are the main task groups? (rough breakdown, you will refine)
   - **Validation** — How will we know the implementation succeeded and is ready to merge?

3. **Create a git branch** — Name it `phase-N/<phase-slug>` matching the roadmap entry (e.g. `phase-1/console-repl-loop`).

4. **Create the spec directory** — `spec/YYYY-MM-DD-<phase-slug>/` using today's date.

5. **Write the three files** using the user's answers plus your reading of `mission.md` and `tech-stack.md`:

   - **`requirements.md`** — Scope, decisions, and context. Sections: Overview, In Scope, Out of Scope, Decisions & Constraints.
   - **`plan.md`** — Numbered task groups (e.g. `## 1. Setup`, `## 2. Core logic`). Each group lists concrete sub-tasks as a checkbox list.
   - **`validation.md`** — Acceptance criteria. Sections: Automated Tests, Manual Checks, Definition of Done.

## Rules

- The `AskUserQuestion` call must happen **before** any file or branch is created. Do not write to disk first and ask second.
- Group all three questions in a **single** `AskUserQuestion` call.
- Use today's date for the directory name (`currentDate` is available in context).
- Keep each file focused and concise — no padding.
- Cross-reference `spec/mission.md` and `spec/tech-stack.md` to ensure the plan stays aligned with project goals and chosen technologies.
