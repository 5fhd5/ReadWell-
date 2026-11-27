# Branching and review model

## Stable trunk

`main` is the only long-lived branch. It should always pass:

```bash
./gradlew clean check
```

Direct feature pushes to `main` are not part of the workflow. Branches are short
lived, focused on one reviewable outcome, and deleted after merge.

## Branch naming

| Prefix | Use | Example |
|---|---|---|
| `feature/` | User-visible capability or architecture change | `feature/cli-persistence` |
| `fix/` | Defect correction | `fix/ambiguous-prefix` |
| `docs/` | Documentation-only change | `docs/demo-notes` |
| `chore/` | Toolchain or repository maintenance | `chore/upgrade-gradle` |

## Workflow

1. Update `main` and create a branch from it.
2. Keep commits conventional and focused:
   - `feat(domain): ...`
   - `test: ...`
   - `fix(cli): ...`
   - `docs: ...`
3. Run `./gradlew clean check` before requesting review.
4. Open a pull request using `.github/pull_request_template.md`.
5. Require a green CI run and one review approval.
6. Merge with a merge commit so the feature boundary stays visible.
7. Delete the feature branch.

## Pull requests in this submission

The history is self-contained and includes four no-fast-forward merges:

```text
*   Merge pull request #4 from feature/static-analysis-docs
|\
| * feat: enforce quality gates and document delivery
|/
*   Merge pull request #3 from feature/test-quality
|\
| * test: cover domain, persistence, and CLI behaviour
|/
*   Merge pull request #2 from feature/cli-persistence
|\
| * feat(cli): add persistent reading-list workflows
|/
*   Merge pull request #1 from feature/catalog-domain
|\
| * feat(domain): add reading-list model and indexed collections
|/
* chore: bootstrap Gradle and repository tooling
```

The review descriptions are stored in `docs/pull-requests/`. This preserves
review rationale and acceptance evidence in the repository even when it is
submitted as a local archive rather than hosted on a specific Git service.

## Commit quality

- Each commit has one purpose and can be described in a short imperative line.
- Generated output and caches are ignored.
- The wrapper, lockfile, source, tests, analysis rules, CI, and docs are tracked.
- Changes to the quality baseline are reviewed like application changes.

