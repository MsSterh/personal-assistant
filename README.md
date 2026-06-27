# Personal assistant

Console application on Clojure for working with LLM that helps users to get answers

## Assistant

- have a personality (described in SOUL.md)
- keep information about user and user actions (MEMORY.md and memory/)
- use this information in responses
- clean up not important information in cron job (HEARTBEAT.md)
- read local file safety
- edit/write local file safety only in current folder
- have to search in internet, read web page content
- create new tasks for cron jobs and run them (HEARTBEAT.md)

## Main files:

- SOUL.md — name, sex, behavior of agent (not editable file for assistant)
- USER.md — information about user: name, timezone, language, preferences
- AGENTS.md — rules for behavior, how to work with files and some restrictions

## Memory

- MEMORY.md — file that consists main and short information about user, some facts and solutions
- memory/ — folder with everyday notes (memory/2026-04-30.md) with topics that agent decided to keep

## System

- HEARTBEAT.md — checklist for cron jobs
- TOOLS.md — additional information about user infrastructure (?)

## Setup

Add the project's `bin/` directory to your `PATH` so you can run `aria` from anywhere:

```bash
# Add to ~/.zshrc (or ~/.bashrc), then restart your shell or `source` it
export PATH="$HOME/projects/personal-assistant/bin:$PATH"
```

Alternatively, symlink the script into a directory already on your `PATH`:

```bash
ln -s "$HOME/projects/personal-assistant/bin/aria" /usr/local/bin/aria
```

## Commands

```bash
# Run the app (from anywhere, once bin/ is on your PATH)
aria

# Or run directly from the project root
clj -M:repl

# Run tests
clj -X:test

# Lint
clj -M:lint

# Check formatting
clj -M:fmt

# Fix formatting
clj -M:fmt-fix
```

## Tests

All functionality must be covered by tests.
