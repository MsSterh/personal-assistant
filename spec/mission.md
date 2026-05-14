# Mission

Build a private, local-first personal assistant for a single user, delivered as a Clojure console application.

## Purpose

Replace scattered notes, reminders, and web searches with one conversational interface that knows the user, remembers what matters, and acts on their behalf — entirely under their control, on their machine.

## Core values

- **Privacy first.** No data leaves the machine except deliberate LLM API calls. All memory, configuration, and task history live in plain files the user owns.
- **Single user, deeply personal.** The assistant has a defined personality (SOUL.md) and accumulates a growing model of the user (USER.md, MEMORY.md). It is not a generic chatbot.
- **Minimal footprint.** A console app with a flat file system. No database, no server, no daemon required beyond a cron job.
- **Reliable over clever.** Features ship only when covered by tests. The heartbeat cron job keeps the system healthy without user intervention.

## Out of scope

- Multi-user or team support.
- A graphical or web UI.
- Cloud storage or sync.
