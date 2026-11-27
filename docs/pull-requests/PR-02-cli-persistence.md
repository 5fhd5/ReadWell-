# PR #2: Persistence and CLI workflows

## Branch

`feature/cli-persistence` -> `main`

## Summary

- Added the repository boundary and atomic JSON file adapter.
- Added the transactional `ReadingListService`.
- Added nine Picocli subcommands, output formatting, and exit-code handling.

## Review focus

- Does persistence complete before in-memory state changes?
- Are destructive operations explicit?
- Are successful output and errors separated between stdout/stderr?

## Acceptance evidence

- Installed distribution can add, list, search, summarize, and show history.
- Data survives across separate invocations using the same file.
- Removal refuses to proceed without `--yes`.

## Decision

Approved after smoke testing and merged with `--no-ff`.

