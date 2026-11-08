# Remaining CWE follow-up plan

## Objective and boundaries
Investigate the user's reported 12 remaining sites on skillhub-security/dev only, retain the prior CVE/Spring Boot 4 fixes, test material behavior changes, and deliver verified dev source with a new annotated tag. The attached mission requests report/tag names dated 2026-11-04; execution date is 2026-11-08 (Asia/Taipei). Do not overwrite existing tags.

## Phases
1. [complete] Baseline and inventory: mapped all 12 supplied line groups to current methods and flows; the report distinguishes current-source issues and false positives from unavailable company traces.
2. [complete] Regression and remediation: URL, independent scan directories and duplicate names reproduced failures before fixes; focused native checks passed. Added actual PostgreSQL tests for the already-bound SQL groups and secure-stream Linux cleanup.
3. [complete] Verification: b9980acf full Java 2777 / 2747 passed / 30 existing skips / zero failures/errors; all new filesystem and PostgreSQL cases ran without skips. E2E 182/3, public Semgrep 199/four unchanged, artifact Critical zero, PR CodeQL passed. Go tested on an identical Go tree. Actual commands/results/hashes are in the report and structured evidence.
4. [recorded externally] Delivery: source integrated locally into dev after checks; publication adds evidence documents only. Final remote dev scan, literal publication SHA and annotated security-semgrep-2026-11-04 tag/peel verification are recorded in the final tag and PR #3. No original company finding IDs or 31/31 closure is invented.

## Constraints
- The user's reported 19 previous fixes and 12 remaining cases are supplied counts, not independently reproduced scanner closures.
- Original company JSON/SARIF/rules are unavailable; do not substitute public-rule counts for company acceptance.
- Never change the Python fork/upstream, generated sources, suppressions, dependency versions or unrelated code.

## Errors
- Optional root CLAUDE.md is absent; applicable target AGENTS and Trellis conventions are being used.
- A baseline JSON read used Windows' cp950 default and failed; all subsequent JSON reads explicitly use UTF-8.
- Docker Desktop Linux engine pipe is unavailable locally. Do not reset Docker or user data; run real PostgreSQL/Linux regressions on the existing GitHub-hosted CI.
- First E2E run 42473763199 stopped before product tests: ubuntu-latest supplied Ubuntu 26.04, unsupported by pinned Playwright 1.58.2. Pin only the E2E runner to supported Ubuntu 24.04; rerun current source without upgrading application dependencies.
- Optional javap bytecode inspection is unavailable in the bundled JBR (no executable or jdk.jdeps module). Source constant-expression review and actual PostgreSQL API regressions remain the evidence; no bytecode claim is made.
- Schema review found the new random credential fixture must fit fingerprint CHAR(16); use 16 random hex digits rather than the full UUID. No production schema or security logic is changed for test data.
- Native workflow-shell-inputs-test.py failed with eight Windows temporary-directory sharing/cleanup errors. The Linux Semgrep workflow runs that existing script successfully; do not count the native attempt as passed or weaken the test.
- Full Linux run 42474395702 found the new filesystem race test requested static mocking through the global subclass MockMaker, which does not support it. Use the inline maker only in that test's MockSettings; retain the global subclass resource and all dependency versions. A standalone Java 21 probe with those per-test settings and real Files calls passed; full Linux rerun is required.
