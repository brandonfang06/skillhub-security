# Requested CWE remediation follow-up — 2026-11-02

This follow-up expands the initial scan-workspace-only delivery at `56f296a931423b37a79fe344349437e15cf27e4c` to every CWE category explicitly requested by the user. The named ZIP and original approximately 31-finding report remain unavailable. The requested annotated tag has not been created; it is deferred until this expanded change is verified and merged.

## Changes by requested category

| Category / source | Implemented change | Verification |
| --- | --- | --- |
| CWE-176 / BuiltinSkillRemotePackageDownloader | Replace Unicode-aware lowercasing/suffix comparison with a fixed ASCII DNS host pattern using ASCII case-insensitive matching. Keep HTTPS, port, user-info, redirect, size and timeout boundaries. | Unicode/fullwidth/encoded/suffix/user-info confusion rejection and uppercase ASCII CDN acceptance; existing downloader tests. |
| CWE-22 / ScanTaskConsumer, SecurityScanService | Initial delivery already confines local paths and cleanup to strict workspace descendants, rejects existing symlink components and validates all names before content/task/audit writes. | Negative tests reproduced original acceptance; Linux symlink read/write/cleanup tests and existing retry/lock/logging tests. |
| CWE-22 / SkillPackagePolicy | Validate portable path segments directly before host filesystem parsing. Reject parent/current/empty segments, Win32 device names (including extensions and superscript COM/LPT aliases), controls, reserved characters, and whitespace/dot aliases; preserve valid Unicode paths and SKILL.md canonicalization. | New test failed against NUL.txt before the fix. Portable traversal, drive, alias and valid Unicode regression coverage. Existing traversal error wording retained. |
| CWE-89 / JdbcGithubSyncHistoryRepository | Remove runtime IN-list SQL formatting. Bind UUID arrays to constant ANY predicates; framework owns array cleanup. | New real PostgreSQL multi-task retention test with SQL control characters as path data; existing retention cases. |
| CWE-89 / JdbcGithubSyncCapacityGate, JdbcGithubSyncUploadRepository, JdbcGithubSyncSchedulePolicyRepository | Use a fixed bound nullable-namespace predicate; choose only closed constant upload lock/read statements and boolean schedule read modes. Remove runtime SQL assembly and arbitrary SQL text from the schedule helper interface. | Existing admission/lease/upload/schedule/retention PostgreSQL integration coverage; full reactor pending. |
| CWE-89 / JdbcGithubSyncTaskRepository | Replace eight manual PreparedStatementCreator paths with constant SQL plus explicitly separated JDBC arguments. Framework-managed text arrays retain capability predicates and release JDBC array resources. | Existing real PostgreSQL lease/renew/admission/discovery/result/retry/concurrency tests; full reactor pending. |
| CWE-89 / JdbcGithubOfficialDiscoveryRepository | Constant SQL and separate bound arguments for task creation and catalog retirement; framework-managed text/UUID arrays. | Existing official discovery, scheduling and quota PostgreSQL integration tests; full reactor pending. |
| CWE-89 / JdbcGithubSyncPathPresenceRepository | Use fixed named-parameter batch SQL instead of a public callback closing over method inputs. Missing-since, timestamps, source scope and batch behavior retained. | Existing real PostgreSQL path-presence cases; full reactor pending. |
| CWE-89 / GithubSyncWorkerPresenceRepository | Constant SQL with bound worker/protocol/capabilities; framework-managed arrays with cleanup. Single-row heartbeat ID is also bound. | New actual JdbcTemplate tests verify hostile strings remain data and arrays are freed after success/database failure. New PostgreSQL quoted-worker/capability round-trip test. |
| CWE-89 / InitialAuthSettingsInitializer | Use a constant prepared advisory-lock statement with a bound bigint key, preserving transaction serialization and lock identity. | New callback test verifies prepared SQL, key binding and closure; existing initializer tests. |
| CWE-601 / AnonymousDownloadIdentityService | Emit a typed servlet cookie rather than using generic response headers; use framework-resolved request security state instead of reading raw forwarding headers again. Preserve signed values, host-only scope, HttpOnly, SameSite, path and default max age. The class still has no redirect operation. | A new transport test first failed on direct raw-header trust. Tamper/reuse/forged-host tests pass. A real loopback Tomcat HTTP test verifies actual Set-Cookie safety attributes and no Location header. |
| CWE-259 / tree_live_test.go | Live tree tests require explicit environment credentials or explicit anonymous mode; remove automatic borrowing of personal gh credentials. Test credentials are runtime-generated. The fixed public commit SHA remains an immutable fixture, not a password. | New unconditional credential-source and anonymous-no-credential-access Go tests; Linux Go test/vet pending. README updated. |

The JDBC changes harden the interface and resource lifecycle even where the original code already bound values. They are not evidence that each old call was exploitable SQL injection. Similarly, a public Git SHA is not a password and the anonymous identity class did not perform redirects. No blanket claim is made that these edits close 31 original report entries without their IDs/traces and the company scanner.

No nosemgrep, rule weakening, dependency downgrade, generated-file edit, API/schema change, authorization bypass or audit removal is used. Existing Spring Boot 4 and Critical-CVE fixes are unchanged.

## Security and verification

C: confine scanner host reads; use explicit test credentials; preserve cookie signing and host scope. I: keep all SQL values separate from query text, preserve source/task scoping and lock predicates, reject portable filename aliases. A: preserve transaction/retry/lease semantics and release JDBC arrays on failure; preserve bounded downloader/network behavior. A service-owned workspace remains required; existing-symlink checks are not a race-free sandbox against privileged filesystem replacement.

Local Java 21 focused reactor: **78 reported, zero failures/errors, 2 Windows symlink skips**. Includes real loopback Tomcat HTTP and actual JdbcTemplate parameter/resource behavior. Command from repository root:

```powershell
.\server\mvnw.cmd -f server/pom.xml -B -pl skillhub-app -am '-Dtest=SkillPackageValidatorTest,SecurityScanServiceTest,ScanTaskConsumerPathSafetyTest,BuiltinSkillRemotePackageDownloaderTest,AnonymousDownloadIdentityServiceTest,AnonymousCookieTransportTest,InitialAuthSettingsInitializerTest,GithubSyncWorkerPresenceSqlSafetyTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Real PostgreSQL, complete Linux Java/Go, E2E and public scanner/dependency verification are pending for this expanded source. Earlier results apply only to the initial delivery and are not represented as newly executed checks for these edits.

Implementation references: [Spring JDBC](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html), [framework-managed SQL arrays](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/jdbc/support/SqlArrayValue.html), [Win32 filename rules](https://learn.microsoft.com/en-us/windows/desktop/fileio/naming-a-file).

## Coverage and transport follow-up

The first expanded full reactor found one failure in an existing MockMvc assertion: Spring mock cookie header rendering omitted the general SameSite servlet attribute. Actual loopback Tomcat HTTP output already passed. Keep the assertion on the typed cookie attribute in mock tests and verify the actual header in the real Tomcat test; no safety assertion is dropped. All four PostgreSQL history/worker tests (including the new hostile values and multiple UUIDs) and both array success/failure tests passed in that run. The complete suite must be rerun before acceptance.

The first expanded public scan removed the PathPresence SQL audit match, but raised two cookie-flag audit warnings because flags set in a helper were not visible at addCookie. The flags now sit directly at the emission boundary. An actual Semgrep CE 1.181.1 native scan using those two pinned official rules reports **0 findings / 0 errors** for the service; the cookie/controller and real HTTP regression tests pass after the move.

A separate coverage audit showed that Semgrep defaults excluded *_test.go, including the specifically requested tree_live_test.go. A project .semgrepignore now retains the upstream dependency/output/test-directory defaults but removes only the Go-test suffix exclusion. It adds no new exclusions. The expanded workflow adds p/golang, and its source gate requires all 61 tracked Go files, including Go tests, and blocks their ERROR/HIGH/CRITICAL findings and parse errors. Three gate tests failed before this change; all 13 now pass. Actual native Semgrep with p/golang + p/secrets ran 78 rules on 65 targets, included tree_live_test.go, and reported **0 findings / 0 errors**. Native Windows support was verified using an isolated uv tool environment; no Docker reset was required.

References: [Semgrep ignore defaults and override behavior](https://docs.semgrep.dev/ignoring-files-folders-code). Public CE rules still do not reproduce the unavailable organization policy or establish 31-of-31 closure.
