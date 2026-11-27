# Ten-minute live demo

This script assumes `./gradlew installDist` has already run. Replace the Unix
executable with `build\install\readwell\bin\readwell.bat` on Windows.

## 0:00-1:00 - Framing

State the problem: a local reading list should help choose a next book, track
progress, and preserve history without requiring a database.

Show:

```bash
git switch main
git log --oneline --graph --decorate -12
```

Call out the four feature branches, merge commits, conventional messages, and
the stable `main` trunk.

## 1:00-2:00 - Toolchain and reproducibility

Show:

```bash
java -version
./gradlew --version
./gradlew clean installDist
```

Point out Gradle 8.14.3, Java 21 bytecode, the committed wrapper, wrapper
checksum, exact dependency versions, and `gradle.lockfile`.

## 2:00-3:15 - Architecture

Open `README.md`, then summarize the flow:

```text
Picocli -> ReadingListService -> LibraryRepository -> atomic JSON
                    |
                    +-> PriorityShelf, TitleTrie, ActivityRing
```

Explain why a tiered `TreeMap` shelf, trie-based title prefix index, and bounded
ring buffer are appropriate without overstating scale.

## 3:15-5:15 - Add, persist, and inspect

Use a temporary data file so the demo is repeatable:

```bash
APP=build/install/readwell/bin/readwell
DATA=/tmp/readwell-demo.json
rm -f "$DATA"

$APP --data "$DATA" add --title "Designing Data-Intensive Applications" \
  --author "Martin Kleppmann" --year 2017 --pages 616 --priority HIGH \
  --tag systems --tag engineering

$APP --data "$DATA" add --title "The Pragmatic Programmer" \
  --author "Andrew Hunt and David Thomas" --year 1999 --pages 352 \
  --priority MEDIUM --tag craft

$APP --data "$DATA" list
$APP --data "$DATA" search design
$APP --data "$DATA" stats
```

Run `list` again in a separate process to show that state came from disk.

## 5:15-6:45 - Lifecycle and validation

Copy the short ID printed by `add`, then:

```bash
$APP --data "$DATA" status <ID> --set READING
$APP --data "$DATA" progress <ID> --pages 200
$APP --data "$DATA" status <ID> --set FINISHED --rating 5
$APP --data "$DATA" show <ID>
$APP --data "$DATA" history --limit 5
```

Demonstrate two failure paths:

```bash
$APP --data "$DATA" progress <ID> --pages 1
$APP --data "$DATA" add --title "Dune" --author "Frank Herbert" \
  --year 1200 --pages 0 --priority HIGH
```

Explain exit codes 3/4/6 and why no partial mutation is visible after failed
persistence.

## 6:45-8:15 - Build and tests

Show the test layout, then run:

```bash
./gradlew test --rerun-tasks
```

Open:

```text
build/reports/tests/test/index.html
```

Call out domain, persistence, parser, and end-to-end CLI coverage.

## 8:15-9:15 - Coverage and static analysis

```bash
./gradlew jacocoTestReport jacocoTestCoverageVerification detekt
```

Open `build/reports/jacoco/test/html/index.html` and
`build/reports/detekt/detekt.html`.

Report the measured baseline: 35 tests, 89.18% line coverage, 73.50% branch
coverage, and zero Detekt findings. Mention the enforced 80% line threshold.

## 9:15-10:00 - Reflection and close

Close with the main tradeoff: indexes are rebuilt after each mutation for
clarity and consistency on a small catalogue. For larger concurrent use, the
next changes would be incremental index maintenance, cross-process locking,
and schema migration.

Finish:

```bash
./gradlew clean check
```

This one command demonstrates the complete quality gate.

