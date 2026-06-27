# Phase 4 — Web Tools: Requirements

## Overview

Give the assistant two new tools so it can reach the open web during a
conversation: a **web search** (query → result snippets) and a **web page
reader** (URL → extracted plain text). Both plug into the existing tool
registry & dispatch layer from Phase 3, so the assistant decides when to call
them just like the file tools.

This is the first feature that makes deliberate network calls beyond the LLM
endpoint. Per the mission, that is acceptable as long as it is explicit and
under the user's control — the only egress is the search query and the URLs the
assistant chooses to fetch.

## In Scope

- `web_search` tool: a text query returns a list of result entries
  (title, URL, snippet) by scraping DuckDuckGo's HTML endpoint — no API key.
- `web_read` tool: a URL is fetched and returned as either readable plain text
  (default) or raw HTML, selected by a `format` argument (`"text"` | `"html"`).
  Text mode uses **jsoup** (scripts/styles dropped, tags stripped, whitespace
  collapsed); HTML mode returns the unprocessed body for when the assistant
  needs markup the text extraction would discard. Output is uniform:
  `{:url :format :content}`.
- An `HttpClient` protocol (mirroring the existing `LLMClient`) with a
  hato-backed implementation, injected into the web tools so tests substitute a
  fake without touching the network.
- Registration of both tools in `tools.registry` (`tools` map + `tool-defs`)
  and dispatch through the existing `dispatch` fn.
- Structured, serializable result maps (and `{:error ...}` on failure) matching
  the convention the file tools already follow.

## Out of Scope

- Any third-party search API requiring a key (Brave, SerpAPI, etc.). DuckDuckGo
  HTML scraping is the chosen backend; the provider is not made pluggable in
  this phase.
- JavaScript-rendered pages / headless browser. `web_read` reads server-returned
  HTML only.
- Caching, rate limiting, or robots.txt handling.
- Authentication, cookies, or POST-based forms when fetching pages.
- Summarizing or chunking fetched content — extracted text (or raw HTML) is
  returned and the LLM handles it.

## Decisions & Constraints

- **Search backend: DuckDuckGo HTML.** Scrape `https://html.duckduckgo.com/html/`
  (the no-JS endpoint). No API key, no config entry required. Accept that this
  is best-effort and may be rate-limited.
- **HTML extraction: jsoup.** Add `org.jsoup/jsoup` to `deps.edn`. Use it both
  to parse DuckDuckGo result rows and to extract readable text in `web_read`.
- **HTTP behind a protocol.** New `HttpClient` protocol with `http-get` (and a
  hato-backed record). The web tools receive a client rather than calling hato
  directly, so tests inject a fake — same approach as `LLMClient` in
  `personal_assistant.llm`.
- **Reuse Phase 3 plumbing.** No changes to `dispatch`'s contract; the new tools
  are ordinary entries in the `tools` map and their schemas join `tool-defs`.
- **Result shape.** Each tool returns a plain map serializable by cheshire;
  errors are `{:error "..."}`, never thrown out of the tool fn.
- **Testing.** All public fns covered by `clojure.test` with a fake
  `HttpClient`; no network access in the suite. clj-kondo and cljfmt clean.
