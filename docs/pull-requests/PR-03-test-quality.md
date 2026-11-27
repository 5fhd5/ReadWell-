# PR #3: Automated test and coverage baseline

## Branch

`feature/test-quality` -> `main`

## Summary

- Added validation and malformed-data tests.
- Added service lifecycle, search, ordering, failure-transaction, and statistics tests.
- Added data-structure tests.
- Added parser, error-path, help/version, and end-to-end CLI tests.
- Enabled JaCoCo reporting and an 80% line-coverage gate.

## Review focus

- Does the suite test behaviour instead of implementation details?
- Are persistence failures covered?
- Are CLI exit codes and argument failures covered?

## Acceptance evidence

- 35 tests pass.
- 89.18% line coverage and 73.50% branch coverage measured.
- `jacocoTestCoverageVerification` passes.

## Decision

Approved and merged with `--no-ff`.

