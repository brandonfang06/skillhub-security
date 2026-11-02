# Source review and verification — 2026-11-02

This report records the initial scan-workspace delivery. The expanded changes for all requested CWE categories and the final verification are tracked in [Requested CWE follow-up](2026-11-02-requested-cwe-follow-up.md); its dispositions supersede the initial audit-only entries below.

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

## Linux verification of source commit 234e3d33

| Check | Actual result / evidence |
| --- | --- |
| Java 21 complete reactor, `make test-backend` (`./mvnw test`) | [PR Tests 41432949875](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949875): 2,759 reported, 2,729 passed, 30 existing conditional skips, zero failures/errors. Both new symlink tests executed with **zero skips** on Linux. |
| Go `go test -count=1 ./...`, `go vet ./...` | [41432949844](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949844): all five packages passed; vet passed. Opt-in public live GitHub fixtures were not enabled. amd64/arm64 image checks also passed. |
| Real service/browser E2E | [41432949888](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949888): 182 passed, 3 skipped. |
| Frontend build/unit tests | [41432949875](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949875): Node 22 and 24 each passed 1,362 tests and build. |
| Semgrep and actual artifact CVEs | [41432949850](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949850): Semgrep gates passed; Java 347 package entries and frontend 737 package entries, **zero Critical CVEs** in each report. No dependency downgrades or ignore-unfixed flag. |
| CodeQL Java/JavaScript | [41432949719](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949719): passed; zero security severity >=9 results, no new rule/path matches in PR SARIF. PR and dev analysis contexts differ, so lower PR counts are not credited as fixes. |
| Additional architecture/migration checks | RISC-V image builds [41432949814](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949814) and migration preflight [41432949927](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949927) passed. |
| Dependency Review action | [41432949841](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949841) failed because GitHub reports Dependency Review unsupported / Dependency Graph unavailable for this repository. No dependency manifests changed. This is **not** counted as a passing check; independent actual-artifact Critical scans above passed. |

### Remaining public findings and analysis limitations

Semgrep CE **1.181.1**, pinned image digest `sha256:3db5c7af55eb1f512eac0abf0ab69a97b52c74b5e9a27314c221aee1a78d7ab2`, ran the existing seven public packs with inline suppressions disabled. It scanned 2,945 files and reported **200 findings**: 193 WARNING, 5 INFO, 1 ERROR and 1 MEDIUM. Every one of the 1,605 expected primary Java/frontend/workflow files was scanned. Primary blocking findings and primary application analysis errors are zero.

The remaining ERROR is the existing DSH HTTP probe at `plugins/dsh/scripts/probe-host.mjs:162`; the MEDIUM is the npm minimum-release-age audit at `web/.npmrc:1`. They are outside this patch and are not silently resolved. The full report retains **71 diagnostics**, including 40 workflow-template partial-parsing diagnostics and 31 outside the primary gate. The rule/path finding multiset matches baseline run 41296194501 exactly; public findings did not disappear through suppression.

The complete official Java rule tree at commit `16b9e6d45f10649f6a531ec4838ed9924372c3bc` ran 123 applicable rules on 1,429 files, including all **1,283 production Java files**. It retains **3 WARNING + 2 INFO**, zero reported analysis errors: Git SHA-1, legacy identifier MD5, constant/bound PathPresence batch SQL, and two GCM audit hints. These are unchanged from baseline and their contexts remain documented in the prior November 1 review. They are review findings, not five proven exploitable vulnerabilities or five completed fixes.

[Compact evidence and report SHA-256 hashes](2026-11-02-remediation/evidence.json) preserve the scope/counts. Raw scanner reports, CVE package reports and SARIF remain in the linked Actions artifacts. Exact final dev commit checks and the peeled tag target are recorded in the annotated tag after merge. No claim is made that the unavailable original approximately 31 findings have all been fixed, or that the unavailable ZIP has been reviewed/applied.

PR [#1](https://github.com/brandonfang06/skillhub-security/pull/1) was merged into dev as `0a2afafccefc2144f0e74c82fad3ba3b8099ddd1`. The second complete Java workflow [41432949819](https://github.com/brandonfang06/skillhub-security/actions/runs/41432949819) also passed (2,759 reported / zero failures/errors / 30 conditional skips). The evidence-only follow-up does not change any production source, test, manifest or workflow. Final exact-dev workflow run IDs/results are retained in the annotated `security-semgrep-2026-11-02` tag; that tag must only be pushed after successful final verification.
