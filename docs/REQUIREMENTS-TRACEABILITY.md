# Assignment requirements traceability

| Assignment requirement | Implementation evidence |
|---|---|
| Kotlin command-line application for a small domain | Reading-list domain in `src/main/kotlin/io/github/readwell/domain`; Picocli entry point in `cli/Main.kt` |
| Documented branching model | `docs/BRANCHING.md`, `.github/pull_request_template.md`, and merge commits on `main` |
| At least four feature branches merged through pull-request reviews | Merge commits #1-#4 and review records in `docs/pull-requests` |
| Readable commit history | Conventional commit messages plus visible no-fast-forward feature boundaries |
| Gradle build and committed wrapper | `build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle/wrapper` |
| Pinned dependencies and clean-clone reproducibility | Exact plugin/dependency versions, `gradle.lockfile`, wrapper checksum, fixed toolchain |
| Sub-commands and argument parsing | Nine Picocli subcommands in `cli/Commands.kt`, parser tests in `cli/CliTest.kt` |
| Input validation | `domain/BookValidation.kt`, domain tests, CLI domain-error tests |
| Error handling | Typed `DomainException` hierarchy, CLI exception handlers, documented exit codes |
| Persistence | Repository boundary plus atomic JSON adapter in `persistence` |
| At least two non-trivial data structures | `PriorityShelf` (`EnumMap` + tier `TreeMap`s), `TitleTrie`; plus bounded `ActivityRing` |
| Domain and CLI tests run by build | JUnit 5 suite executed by Gradle `test`, included through `check` |
| Coverage reporting | JaCoCo HTML/XML report and enforced line threshold |
| Static analysis with no unresolved findings | Detekt plugin/config, `check` dependency, zero-finding result |
| README purpose/instructions/architecture/results/reflection | `README.md` |
| Ten-minute live demo | `docs/DEMO.md` |

