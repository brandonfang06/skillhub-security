# Source review and verification — 2026-11-02

Destination: `brandonfang06/skillhub-security`, branch `dev`. Starting commit: `8c9c085659a412f61f64621e988b1c1fdd017e89`; upstream source base remains `326cbd62244aac7dbd7a97a136148231d6a46ce6` (v0.2.25). The separate Python repository is untouched.

## Findings review

| Requested category | Actual code and disposition |
| --- | --- |
| CWE-22 / SecurityScanService | Confirmed inconsistent path policy: local and upload modes accepted noncanonical parent segments. On Linux the upload validator could also accept nested backslash traversal; it validates names but does not itself build the archive, so this is defense in depth rather than proof of a public upload exploit. Reuse existing SkillPackagePolicy for both modes before content access or audit/task writes. Reject pre-existing symlink components before directory creation and file copying. |
| CWE-22 / ScanTaskConsumer | Confirmed queue local path passed directly to scanner; cleanup accepted the workspace root and symlink ancestors. Require a strict workspace descendant with no existing symlink components for local scan/cleanup; refuse a symlinked staging root. |
| CWE-22 / SkillPackagePolicy | Existing policy rejects traversal, absolute/drive-qualified and noncanonical paths. Production policy unchanged; additional portable-separator regression coverage. |
| CWE-176 / BuiltinSkillRemotePackageDownloader | Host comes from URI.getHost, compared using Locale.ROOT against the CDN boundary; redirects disabled. No Unicode host bypass reproduced. Added Unicode, encoded-host, user-info and suffix-confusion rejection tests plus uppercase ASCII acceptance. No vulnerability closure claimed for the unknown original finding. |
| CWE-89 / JdbcGithubSync repositories | Audited construction sites. Values are bound; History builds only count-derived `?` placeholders, CapacityGate appends only fixed predicate text, Upload appends only fixed locking text. PathPresence batch uses PreparedStatement setters with constant SQL. No demonstrated injection; retain public audit warning instead of suppression or cosmetic query changes. |
| CWE-89 / GithubSyncWorkerPresenceRepository | Fixed SQL and prepared bindings, including PostgreSQL array parameters. No demonstrated injection; no production edit. |
| CWE-89 / InitialAuthSettingsInitializer | Advisory-lock SQL and user count are constants. Role grant values go through repository persistence. No demonstrated injection; existing initializer tests included. |
| CWE-601 / AnonymousDownloadIdentityService | Class issues/verifies cookies and does not redirect. Added forged-host/no-Location, host-only cookie, valid reuse and tamper rejection tests. No demonstrated open redirect; no production edit. |
| CWE-259 / tree_live_test.go | Fixed literal is a public Git commit SHA; tokens come from environment or local gh authentication and are not logged. No embedded password found. Linux Go tests required because acquisition code uses Linux syscalls. |

The original approximately 31 organization findings and their rule IDs, traces, scanned SHA, severities and rules were not obtained. Their individual status remains **unverified**. GitHub Code Scanning exposes CodeQL findings, which are not substitutes for that set. The named remediation ZIP was not found; no README/Python script from it was available or executed, and no claim is made that its patch was reviewed/applied.

## Regression and security boundaries

Negative tests failed before fixes: domain suite 23 tests / 2 failures (noncanonical path acceptance); consumer suite 2 tests / 1 failure (unsafe local path accepted). Tests cover both scan modes, path validation before any content access, separators/drive prefixes, workspace root and sibling-prefix rejection, symlink parent read/write/cleanup, existing successful scan/retry/lock/logging behavior, CDN host validation and signed cookie behavior.

A previous lock-release test used `/tmp/failure`; it now uses a valid workspace path so it still reaches and asserts the scanner failure and lock release. No assertion was removed.

C: reject arbitrary host reads from queue paths. I: reject path aliases and existing symlink escapes before writes; reject cleanup of the shared root. A: retain attempt locks, retry/ACK behavior, audit ordering and ordinary successful scans. The workspace must remain owned by the service. These checks are not a race-free sandbox against privileged concurrent filesystem replacement. No API, schema, generated files, dependency versions or authorization policy changed. Spring Boot 4 / prior Critical-CVE version fixes are preserved.

## Verification status

Exact commands/results and published-commit CI evidence will be recorded here before tagging. Local Docker startup failed in inference-manager socket initialization. Native Windows Go `go test -count=1 ./...` failed to compile Linux-specific syscall code; no Go result is claimed from it. The Python source gate tests passed (10); hostile Bash workflow-input tests encountered Windows temporary-directory cleanup errors (8 subcases), requiring Linux rerun. No machine reset, shared data deletion or dependency downgrade was used.

Existing backend-tests and PR GitHub Sync workflows gain manual dispatch so the exact dev commit can be rerun on Linux. Their test/build commands and permissions are unchanged. Semgrep uses the existing pinned image, public packs, official Java tree, suppression-disabled scans and per-file coverage gate. Public-engine acceptance cannot establish organization acceptance.

Local Java verification (JBR 21.0.9 / Maven wrapper 3.9.13):

```powershell
cd server
.\mvnw.cmd -B -pl skillhub-app -am '-Dtest=SecurityScanServiceTest,ScanTaskConsumerPathSafetyTest,ScanTaskConsumerTest,ScanTaskConsumerLoggingTest,BuiltinSkillRemotePackageDownloaderTest,AnonymousDownloadIdentityServiceTest,SkillPackageValidatorTest,InitialAuthSettingsInitializerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -B -pl skillhub-domain -am '-Dtest=SecurityScanServiceTest,SkillPackageValidatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

The app reactor passed with 90 tests reported, zero failures/errors, two Windows symlink-permission skips. The final domain rerun adds two nested-backslash cases; Linux CI must execute both symlink tests without skipping. `git diff --check` passes. Raw local logs are retained outside Git in the adjacent `skillhub-security-evidence` directory; CI provides shareable Linux evidence.
