# PR #1: Catalogue domain and indexed collections

## Branch

`feature/catalog-domain` -> `main`

## Summary

- Added immutable book, activity, filter, and statistics models.
- Added a typed domain-error hierarchy.
- Centralized title/author/year/page/tag/rating validation.
- Added `PriorityShelf`, `TitleTrie`, and bounded `ActivityRing`.

## Review focus

- Are validation limits and messages understandable to a CLI user?
- Does priority/title ordering remain deterministic?
- Does the ring buffer evict only the oldest entry?

## Acceptance evidence

- Domain model compiles under warnings-as-errors.
- Priority and trie operations are explicitly unit tested in PR #3.
- Data-structure choices are described in the README architecture section.

## Decision

Approved and merged with `--no-ff` to preserve the feature boundary.

