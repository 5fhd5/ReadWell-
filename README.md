# ReadWell

ReadWell is a Kotlin command-line reading-list manager for a single local user.
It keeps a prioritized shelf, tracks reading progress and ratings, searches the
catalogue, records a bounded activity history, and persists everything to a
human-readable JSON file.

The project is delivered with a pinned Gradle wrapper, locked dependencies,
35 automated tests, JaCoCo coverage enforcement, Detekt static analysis, a
documented branch/PR workflow, and a ten-minute demo plan.

## Purpose

ReadWell answers three practical questions without a database or hosted service:

1. Which unread book should I pick up next?
2. How far through each book am I?
3. What changed recently in my reading list?

The small domain keeps the architecture easy to demonstrate while still
exercising argument parsing, validation, error handling, persistence, indexing,
and meaningful automated verification.

## Quick start

Requirements:

- JDK 21 or newer
- No machine-wide Gradle installation

Windows PowerShell:

```powershell
.\gradlew.bat clean check
.\gradlew.bat installDist
$app = ".\build\install\readwell\bin\readwell.bat"
& $app add --title "The Left Hand of Darkness" --author "Ursula K. Le Guin" `
  --year 1969 --pages 300 --priority HIGH --tag science-fiction
& $app list
```

macOS/Linux:

```bash
./gradlew clean check
./gradlew installDist
./build/install/readwell/bin/readwell add \
  --title "The Left Hand of Darkness" \
  --author "Ursula K. Le Guin" \
  --year 1969 --pages 300 --priority HIGH --tag science-fiction
./build/install/readwell/bin/readwell list
```

Data defaults to `~/.readwell/library.json`. Pass `--data FILE` before the
sub-command to use another file:

```text
readwell --data ./demo.json list
```

## Commands

| Command | Purpose | Example |
|---|---|---|
| `add` | Add a validated book | `readwell add --title Dune --author "Frank Herbert" --year 1965 --pages 412 --priority HIGH --tag classic` |
| `list` | Filter and order the shelf | `readwell list --status READING --sort PRIORITY` |
| `search` | Search title prefixes, authors, titles, or tags | `readwell search herbert` |
| `show` | Show one book in detail | `readwell show 4f2c1a9e` |
| `status` | Change state through the lifecycle | `readwell status 4f2c1a9e --set READING` |
| `progress` | Record the current page | `readwell progress 4f2c1a9e --pages 120` |
| `history` | Show bounded activity history | `readwell history --limit 20` |
| `stats` | Summarize status, pages, ratings, and tags | `readwell stats` |
| `remove` | Remove with explicit confirmation | `readwell remove 4f2c1a9e --yes` |

Use `readwell --help` or `readwell <command> --help` for generated help.

Supported workflow:

```text
TO_READ -> READING -> FINISHED
    |          |
    +-> FINISHED
READING <-> TO_READ
FINISHED -> READING (start a reread; progress and rating reset)
```

A rating from 1 to 5 may be supplied only when changing to `FINISHED`.

## Validation and errors

ReadWell rejects blank or oversized titles/authors, control characters,
implausible publication years, invalid page counts, progress outside the book
length, invalid ratings, malformed tags, ambiguous ID prefixes, duplicate
title/author pairs, and invalid state transitions.

| Exit code | Meaning |
|---:|---|
| 0 | Success |
| 2 | CLI usage or argument error |
| 3 | Input validation failure |
| 4 | Book not found / ambiguous ID prefix |
| 5 | Duplicate book conflict |
| 6 | Invalid reading-state transition |
| 7 | Persistence/read-format failure |

Errors are written to stderr. Successful command data is written to stdout.

## Architecture

```text
Picocli commands
      |
      v
ReadingListService  ---- ActivityRing
      |               ---- PriorityShelf
      |               ---- TitleTrie
      v
LibraryRepository interface
      |
      v
FileLibraryRepository -> atomic JSON file
```

Package responsibilities:

- `io.github.readwell.cli`: command parsing, formatting, exit-code translation.
- `io.github.readwell.domain`: immutable model, validation, service rules,
  indexes, bounded history, and statistics.
- `io.github.readwell.persistence`: storage contract and atomic JSON adapter.

Two intentionally non-trivial data structures are part of normal operation:

- `PriorityShelf` is an `EnumMap` of tier-specific `TreeMap` instances. It
  provides high-to-low priority traversal and case-insensitive title ordering
  inside each tier.
- `TitleTrie` is a prefix tree whose nodes retain matching book IDs. It makes
  case-insensitive title-prefix lookup proportional to query length plus match
  count.

`ActivityRing` adds a third useful structure: a fixed-capacity circular buffer
that evicts old events without unbounded memory growth.

State changes follow a write-before-commit sequence: a candidate state is saved
to a sibling temporary file, atomically moved into place where supported, and
only then replaces the in-memory state and indexes. A failed write therefore
does not silently mutate the running catalogue.

## Testing and analysis

Run the complete build:

```bash
./gradlew clean check
```

`check` runs:

- 35 JUnit 5 tests covering validation, domain rules, all three data structures,
  persistence, CLI parsing, CLI error paths, and end-to-end command workflows.
- JaCoCo report generation and an enforced 80% minimum line coverage.
- Detekt with the project rule set in `config/detekt/detekt.yml`.
- Kotlin compilation with warnings treated as errors.

Useful focused commands:

```bash
./gradlew test jacocoTestReport
./gradlew detekt
./gradlew jacocoTestCoverageVerification
```

Reports:

- `build/reports/tests/test/index.html`
- `build/reports/jacoco/test/html/index.html`
- `build/reports/detekt/detekt.html`

### Measured results

Last verified on 2026-11-27 with Gradle 8.14.3, Kotlin 2.2.0, and JDK 24
compiling to Java 21 bytecode:

- Tests: **35 passed, 0 failed, 0 skipped**
- Line coverage: **89.18%** (536 covered / 601 total)
- Branch coverage: **73.50%** (172 covered / 234 total)
- Detekt: **0 findings**
- `clean check`: **pass**

## Reproducibility

- `gradle/wrapper` contains the committed Gradle 8.14.3 wrapper.
- The wrapper distribution has a pinned SHA-256 checksum.
- All application, test, compiler, and analysis dependency versions are exact.
- `gradle.lockfile` locks every resolvable configuration.
- Java bytecode targets release 21 through the Kotlin JVM toolchain.
- Maven resolution uses a pinned mirror first and Maven Central as a fallback.

For a clean-clone check:

```bash
git clone <repository-url>
cd readwell
./gradlew clean check
```

The Gradle binary and dependency caches are intentionally untracked.

## Branch and review workflow

`main` is the stable branch. Work is developed on short-lived branches and
merged with a no-fast-forward merge after review:

- `feature/catalog-domain` -> PR #1
- `feature/cli-persistence` -> PR #2
- `feature/test-quality` -> PR #3
- `feature/static-analysis-docs` -> PR #4

The repository history contains the four branch merge commits. Review records
are in [`docs/pull-requests`](docs/pull-requests), and the full workflow is in
[`docs/BRANCHING.md`](docs/BRANCHING.md). Commit messages follow Conventional
Commits.

## Demo

The timed ten-minute live-demo sequence, including commands and talking points,
is in [`docs/DEMO.md`](docs/DEMO.md). Requirement-to-evidence mapping is in
[`docs/REQUIREMENTS-TRACEABILITY.md`](docs/REQUIREMENTS-TRACEABILITY.md).

## Reflection

The hardest design decision was keeping the application small without making
the evidence superficial. A flat book list would satisfy persistence, but it
would not demonstrate meaningful data-structure choices. The tiered shelf and
title trie keep finding, ordering, and searching part of the domain while
remaining understandable in a live demonstration.

I also treated persistence as a transaction boundary. Early in development it
was tempting to mutate the in-memory list and save afterwards; that risks a
successful CLI message after a failed write. Saving a candidate state first
made the failure rule explicit and testable.

The tradeoff is that each mutation rebuilds the small indexes. For a personal
reading list this is simpler and safer than incremental index repair, and it
keeps update code easy to audit. If the catalogue grew to tens of thousands of
books, the next step would be incremental insert/remove operations and file
locking for concurrent processes.

The final quality pass changed the code, not only the reports: Detekt findings
led to named layout constants, consolidated validation errors, and a documented
suppression at the Java vararg CLI boundary. Tests found Windows-specific temp
directory assumptions and replaced them with repository-local test paths.

See [`docs/BRANCHING.md`](docs/BRANCHING.md) for contribution and review rules.

