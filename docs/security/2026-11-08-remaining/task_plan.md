# Remaining CWE follow-up plan

## Objective and boundaries
Investigate the user's reported 12 remaining sites on skillhub-security/dev only, retain the prior CVE/Spring Boot 4 fixes, test material behavior changes, and deliver verified dev source with a new annotated tag. The attached mission requests report/tag names dated 2026-11-04; execution date is 2026-11-08 (Asia/Taipei). Do not overwrite existing tags.

## Phases
1. [complete] Baseline and inventory: mapped all 12 supplied line groups to current methods and flows; the report distinguishes current-source issues and false positives from unavailable company traces.
2. [pending] Regression and remediation: public URL generation, consumer scan/cleanup, and repository public APIs are the test boundaries already requested in the supplied mission. Use focused red/green tests for changed behavior; real PostgreSQL for database semantics. Do not rewrite safe constant SQL merely for a scanner.
3. [pending] Verification: focused tests, full Java/Go and applicable E2E/SQL tests, pinned public Semgrep before/after, CodeQL and actual artifact Critical scan. Preserve raw evidence outside Git and record exact results.
4. [pending] Delivery: review scoped diff; commit/push feature branch, integrate dev after checks, verify exact dev SHA, create/push unique annotated security-semgrep-2026-11-04 tag with explicit limits. Record final evidence without inventing original finding IDs or 31/31 closure.

## Constraints
- The user's reported 19 previous fixes and 12 remaining cases are supplied counts, not independently reproduced scanner closures.
- Original company JSON/SARIF/rules are unavailable; do not substitute public-rule counts for company acceptance.
- Never change the Python fork/upstream, generated sources, suppressions, dependency versions or unrelated code.

## Errors
- Optional root CLAUDE.md is absent; applicable target AGENTS and Trellis conventions are being used.
- A baseline JSON read used Windows' cp950 default and failed; all subsequent JSON reads explicitly use UTF-8.
- Docker Desktop Linux engine pipe is unavailable locally. Do not reset Docker or user data; run real PostgreSQL/Linux regressions on the existing GitHub-hosted CI.
