# Remaining Semgrep CWE investigation

Execution date: 2026-11-08 (Asia/Taipei). The filename and proposed tag retain the date requested in the attached mission. Repository: brandonfang06/skillhub-security; base dev at dadacdd4b18f97c2e48e90dd36f1e68ede4a4166; branch codex/security-remaining-cwes-2026-11-08.

## Evidence and acceptance

The user reports 19 previous fixes and 12 remaining cases. The attachment supplies filenames and line groups, not original finding IDs, severity, rule definitions or source-to-sink traces. The twelve local review IDs below organize those supplied groups; they are not fabricated Semgrep IDs. Company-rule severity and acceptance remain unverified. The previous public scan and the user's reported 19-case acceptance are distinct evidence sources.

No original remediation ZIP/script is available or executed. No upstream or unrelated Python repository is modified. Prior Spring Boot 4/Critical CVE dependency remediation must remain unchanged. No nosemgrep or exclusion is added.

## Twelve-site inventory (current-source review)

| Local ID | CWE / supplied location | Source to sink and current classification |
| --- | --- | --- |
| R01 | 176, SkillWebUrls.java:32 | Deployment property -> URI.toASCIIString -> generated detail URL. Reproduced NFC conversion changes distinct Unicode prefix paths. Defense-in-depth/path integrity issue; no demonstrated attacker-controlled host or open redirect. Replace implicit normalization with encoding that preserves code points and existing escapes; reject malformed UTF-16. Native red/green and malicious/legitimate URL tests pass. |
| R02 | 22, ScanTaskConsumer.java:350 cleanupTempPath | Queue local path -> guarded recursive deletion. Prior strict descendant/ancestor-symlink checks prevent static traversal. Add descriptor-relative NOFOLLOW_LINKS deletion where the provider supports it. Reproduced same-version scan attempts sharing/overwriting one directory; use private unique temporary directories per attempt. Reject duplicate normalized names before reads/writes. Defense-in-depth plus a confirmed attempt-isolation flaw, not a demonstrated arbitrary remote file-deletion exploit. Original trace unverified. |
| R03 | 89, JdbcGithubSyncUploadRepository.java:230-238 | Private boolean selects one of two compile-time constant queries; UUID, generation, commit, path, token and time are separate bound arguments. Already hardened in prior delivery; current source has no untrusted SQL syntax. False positive against current code; verify existing upload/locking tests. |
| R04 | 89, JdbcGithubSystemCredentialRepository.java:29-30 | list(): SELECT + private static final FIELDS; all concatenated operands are compile-time literals, no user SQL. False positive. |
| R05 | 89, JdbcGithubSystemCredentialRepository.java:44-45 | find(UUID): constant column list/query with bound UUID. False positive. |
| R06 | 89, JdbcGithubSystemCredentialRepository.java:58-65 | availableForClaim: constant query, timestamp/resource bindings, strict quota-resource allowlist, FOR UPDATE SKIP LOCKED. False positive. Added real PostgreSQL hostile-value/allowlist tests; Linux execution pending. |
| R07 | 89, JdbcGithubDiscoveryEntryRepository.java:42-44 | listPlatformEntries: compile-time constant column list; bound catalog namespace ID. False positive. |
| R08 | 89, JdbcGithubDiscoveryEntryRepository.java:49-52 | listTeamRepositories: constant SQL; bound namespace; fixed type/order. False positive. |
| R09 | 89, JdbcGithubDiscoveryEntryRepository.java:57-59 | findTeamRepository: constant SQL; namespace and UUID bindings; fixed repository type. False positive. |
| R10 | 89, JdbcGithubDiscoveryEntryRepository.java:64-69 | findPlatformEntry: constant SQL; UUID and catalog namespace bindings. False positive. |
| R11 | 89, JdbcGithubDiscoveryEntryRepository.java:334-335 returning helper | Private helper receives only literal SQL concatenated with private constant COLUMNS; all values use Object... bindings. No external SQL caller. False positive. Added real PostgreSQL quoted title/owner/repository/JSON and namespace isolation tests; Linux execution pending. |
| R12 | 89, InitialAuthSettingsInitializer.java:62-70 | Fixed SELECT pg_advisory_xact_lock(?) prepared statement; fixed long bound with setLong; no external SQL input; statement closed and transaction serialization retained. Already hardened in prior delivery; current-source false positive. Existing advisory-lock regression will be rerun. |

Original company severity/source/sink traces are unavailable for every row. The source-to-sink descriptions above are independent code review findings, not reconstructed organization traces. Current-code false-positive classification does not prove that the organization's older scanned revision or unknown rule matches are closed.

## Changes and regression coverage

- SkillWebUrls: preserve composed/decomposed paths, Kelvin sign and supplementary Unicode paths; keep existing percent escapes, mixed-case ASCII hosts, deployment prefixes and IPv6. Reject Unicode authority ambiguity, query/fragment/user-info, invalid ports and malformed UTF-16. Scheme comparison remains safe because URI parsing already restricts schemes to ASCII syntax.
- SecurityScanService: private provider-created temporary directory per scan attempt instead of a shared version-ID directory. Both scans retain their own content; stale per-version symlinks are not reused. A normalized-name set rejects duplicate slash/backslash aliases in both local/upload modes before opening any content or writing audit/task/version state. Existing valid nested package behavior remains.
- ScanTaskConsumer: retain strict lexical containment/root rejection and existing symlink checks. Linux secure streams open each parent with NOFOLLOW_LINKS from the filesystem root and delete descendants relative to open handles. Preserve task completion, retry cleanup and best-effort idempotent cleanup. Regression completion removes only its nested tree, leaves a sibling task file intact and preserves an external symlink target.
- GithubReportedSqlSafetyTest: isolated PostgreSQL, migrations, repository APIs, hostile credential labels, allowlisted quota resources, quoted repository/title/JSON data and cross-namespace lookups. Existing repository/concurrency/upload/advisory-lock suites remain required. No production SQL rewrite is justified for the ten already-safe SQL groups.

## Commands and actual verification

Run from the repository root using Java 21 (local Windows uses the existing JBR 21 installation). Native test logs are retained outside Git.

| Command / scope | Actual result at implementation checkpoint |
| --- | --- |
| `server/mvnw.cmd -f server/pom.xml -B -pl skillhub-app -am -Dtest=SkillWebUrlsTest -Dsurefire.failIfNoSpecifiedTests=false test` before fix | One regression failed: expected decomposed `cafe%CC%81`, received composed `caf%C3%A9`. |
| `server/mvnw.cmd -f server/pom.xml -B -pl skillhub-domain -am -Dtest=SecurityScanServiceTest#triggerScan_keepsSeparateAttemptsForTheSameVersionInIndependentDirectories -Dsurefire.failIfNoSpecifiedTests=false test` before fix | Failed: both published tasks shared the same path. |
| `server/mvnw.cmd -f server/pom.xml -B -pl skillhub-domain -am -Dtest=SecurityScanServiceTest#triggerScan_rejectsDuplicatePortableEntryNamesBeforeReadingContent -Dsurefire.failIfNoSpecifiedTests=false test` before fix | Both local/upload cases failed: content opened or duplicate package accepted. |
| `server/mvnw.cmd -f server/pom.xml -B -pl skillhub-app -am -Dtest=SecurityScanServiceTest,SkillWebUrlsTest,SkillWebUrlControllerTest,InitialAuthSettingsInitializerTest,ScanTaskConsumerTest,ScanTaskConsumerPathSafetyTest -Dsurefire.failIfNoSpecifiedTests=false test` after fixes | **60 reported, 57 passed, 3 Windows symlink-permission skips, zero failures/errors.** Linux must run the symlink cases without skips. |
| `python scripts/tests/source-security-gate-test.py` | **13 passed**; primary coverage/findings/analysis gate remains unchanged. |
| `docker version --format '{{json .Server.Version}}'` | Failed locally: Docker Desktop Linux-engine named pipe absent. No Docker/data reset performed. Real PostgreSQL and Linux filesystem verification move to GitHub-hosted CI. |
| `git diff --check` | Passed. Prior dependency versions/locks and generated sources remain unchanged. |

Pre-change [security workflow 42473139942](https://github.com/brandonfang06/skillhub-security/actions/runs/42473139942), exact baseline dadacdd4: eight public Semgrep packs **199 matches** (192 WARNING, 5 INFO, 1 ERROR, 1 MEDIUM), 71 non-primary/template diagnostics; official Java **4 matches** (2 WARNING, 2 INFO), zero diagnostics. **All six production files in the twelve-site inventory already had zero public-rule matches before this follow-up.** Consequently, a later zero-match result in those files cannot be credited as twelve newly eliminated public findings. Actual baseline artifact Critical CVEs are zero across 347 JAR and 737 frontend package entries.

The committed workflow uses Semgrep CE 1.181.1, image `sha256:3db5c7af55eb1f512eac0abf0ab69a97b52c74b5e9a27314c221aee1a78d7ab2`, eight public packs including Go/secrets, and official Java rules commit `16b9e6d45f10649f6a531ec4838ed9924372c3bc`. It disables inline suppressions and enforces the complete primary-source manifest. The organization ruleset is unavailable. Final comparison must match rule ID/path/location or code context and inspect actual findings; raw total changes are not a closure count.

## Remaining risks and final delivery evidence

On providers without SecureDirectoryStream, including the local Windows default provider, the compatible guarded pathname cleanup remains dependent on a service-owned workspace and is **not race-free against concurrent local filesystem replacement**. Do not expose that workspace to untrusted OS accounts. Secure streams improve cleanup on supported Linux filesystems; they do not turn external scanner pathname reads or concurrent privileged filesystem writes into a sandbox.

Current-source review classifies ten SQL groups as safe/false positive and strengthens two remaining URL/filesystem groups. This does not establish company acceptance of 12/12 or 31/31. The user-reported previous 19 are retained as supplied information. Original source/rule IDs, severity and traces remain unverified. No confirmed embedded password or redirect sink was found in the previously reviewed Go fixture/anonymous cookie service; previous CWE-176 downloader, portable path-policy, SQL, cookie and Go changes remain intact.

Final Java reactor/PostgreSQL, Go tests/vet, real-services E2E, Semgrep comparison, CodeQL and actual artifact Critical CVE results will be recorded with the exact final published SHA in the annotated tag and linked PR, after checks complete. This avoids claiming that the preceding local checkpoint is final Linux verification.

- [Remediation branch](https://github.com/brandonfang06/skillhub-security/tree/codex/security-remaining-cwes-2026-11-08)
- [Delivery dev](https://github.com/brandonfang06/skillhub-security/tree/dev)
- Proposed annotated tag: [security-semgrep-2026-11-04](https://github.com/brandonfang06/skillhub-security/tree/security-semgrep-2026-11-04). This link is a planned destination until remote creation is verified. The final tag annotation records its peeled commit SHA and evidence; an existing tag is never overwritten.

Test public detail URL generation, triggerScan, consumer processing/completion, and repository APIs. Run PostgreSQL regressions on isolated Testcontainers; no mock substitutes for SQL execution or locks. Record exact commands, red/green results, scanner versions/rules, remaining findings and actual final SHA/CI/tag links here or in the final annotation/PR evidence after checks finish. Never count changed files or tests as closed findings.

## Sources

- [OpenJDK 21 URI implementation](https://raw.githubusercontent.com/openjdk/jdk21u/master/src/java.base/share/classes/java/net/URI.java): ASCII serialization normalizes non-ASCII text to NFC before encoding; the native Java probe reproduces the changed path.
- [JDK 21 SecureDirectoryStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/SecureDirectoryStream.html): descriptor-relative filesystem operations and NOFOLLOW_LINKS support, subject to provider availability.

## Status

Investigation in progress. No final test, scanner, integration or tag claim is made in this draft.
