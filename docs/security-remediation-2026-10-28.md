# SkillHub security reference — 2026-10-28

Local verification complete. Public destination: [brandonfang06/skillhub-security, dev](https://github.com/brandonfang06/skillhub-security/tree/dev).

This independent reference repository preserves the official Java/React source and Git history from `iflytek/skillhub` main commit `9ebb99226dd1b3e1d4fd8dd3e36baae70a4ed273`. The source already includes application release [v0.2.24](https://github.com/iflytek/skillhub/releases/tag/v0.2.24) and CLI release [cli-v0.2.0](https://github.com/iflytek/skillhub/releases/tag/cli-v0.2.0). The separate Python implementation is not included.

## Acceptance and scope

The user's primary target is Critical issues in Java backend and JS/TS frontend code/dependencies for an internal Semgrep scan. The organization's rules and command were unavailable; the user authorized best-effort public-rule verification. Other severities and container OS findings are not the primary acceptance gate.

- Semgrep public production-code gate: **0 ERROR findings, 0 parser errors**.
- Broader public SAST audit: **4 WARNING findings, all metadata impact MEDIUM**.
- Actual delivery JAR: **0 Critical CVEs**, down from **14** in the upstream artifact.
- Frontend lockfile: **0 Critical CVEs**; baseline npm audit also reported zero vulnerabilities at all severities.
- Supplemental built server, web and scanner images: **0 Critical CVEs each**. Scanner initially had two Critical OS findings.

Semgrep ERROR is its highest CLI severity and is not equivalent to CVSS Critical. This report does not guarantee that an unknown organization policy passes, or that the repository contains no vulnerabilities at any severity.

## Remediation

| Component | Upstream | Verified delivery |
| --- | --- | --- |
| Spring Boot | 3.2.3 | 4.0.9 |
| Spring MVC / WebFlux | 6.1.4 | 7.0.10 |
| Spring Security Web | 6.2.2 | 7.0.8 |
| Embedded Tomcat | 10.1.19 | 11.0.27 |
| Netty | 4.1.107.Final | 4.2.19.Final |
| Bouncy Castle provider | 1.78.1 | 1.85 |

The [Spring MVC advisory](https://spring.io/security/cve-2026-47884/) and [WebFlux advisory](https://spring.io/security/cve-2026-47892/) identify Spring 7.0.9 as the OSS fix; older fixed branches require enterprise support. The vendor rates those advisories Medium while the CVE database used here rates the installed dependencies Critical; the fixes were applied rather than suppressing the findings.

Spring Boot 4 compatibility changes retain Jackson 2 JSON behavior and OpenAPI 3.0 output, move framework imports and Mockito test overrides, adopt supported security request matchers and the RestClient token client, preserve bounded OAuth responses, preserve DingTalk's provider-specific lack of PKCE support, and update the OTLP property names. Existing `SKILLHUB_OTLP_TIMEOUT`/`SKILLHUB_OTLP_COMPRESSION` aliases remain supported; direct `MANAGEMENT_OTLP_TRACING_ENDPOINT` configuration becomes `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT`. Deployment examples and operator documents match.

Two Semgrep ERROR audit alerts concerned assembled JPQL whose values were already parameter-bound. The list/count queries now use fixed JPQL with named parameters throughout. New real-PostgreSQL tests cover combined filters, pagination/count consistency, and SQL control characters as data. These audit alerts are not presented as newly assigned CVEs or proven exploitable injection.

Two equivalent source expressions were adjusted so the public Semgrep parser handles Java profile-result matching and a generated TypeScript return type without partial parsing. No generated API files, security rules, `nosemgrep` annotations or broad finding suppressions were added.

The test-only H2 dependency is pinned to 2.5.252 to fix the [upstream closed-session/check-constraint regression](https://github.com/h2database/h2database/issues/4291) encountered after the framework upgrade. Runtime PostgreSQL remains the validation database for migrations and transactions.

Supplemental container work pins base image digests, moves the scanner to Python 3.13/Trixie, and records its complete installed dependencies in `scanner/requirements.txt`. Shell files use LF for reliable image startup on Windows checkouts.

## SAST evidence and remaining audit findings

Semgrep OSS 1.181.1, image digest `sha256:3db5c7af55eb1f512eac0abf0ab69a97b52c74b5e9a27314c221aee1a78d7ab2`, used fresh public `p/java`, `p/javascript`, `p/typescript`, and `p/security-audit` rules. Metrics were disabled and code was scanned locally in the tool container.

The broad audit ran 176 applicable rules on 1,716 files, with zero parser errors. The ERROR-only gate ran 50 applicable rules on 1,579 files, with zero findings and zero parser errors. Frontend `*.test.ts`/`*.test.tsx` fixtures are excluded from this production-code gate; backend scope is `src/main`. No production-code finding was excluded to make the gate pass.

The engine also emitted 48 fixpoint diagnostics in the broad run and 46 in the ERROR-only run. Those data-flow analysis limits are retained in the JSON evidence; zero reported findings is not proof that every path was exhaustively analyzed. Proprietary organization rules and interfile/Supply Chain policies were not available.

| Remaining public audit warning | Review / retained behavior |
| --- | --- |
| SHA-1 in `GithubSyncArchiveValidator` | Git object SHA compatibility; not substituted for the package's stronger content digest or password/token hashing. |
| MD5 in `LegacyOAuthIdentityCoordinate` | Legacy deterministic identity coordinates; changing them requires a migration and is outside this Critical-only task. |
| SQL warning in `JdbcGithubSyncPathPresenceRepository` | The statement is constant and values use `PreparedStatement` bindings. |
| `dangerouslySetInnerHTML` in `code-renderer.tsx` | Code text is HTML-escaped; highlight markup/class names come from registered lowlight grammars. |

Non-Critical dependency findings also remain. The supplemental server image all-severity scan reports 1 High, 29 Medium and 4 Low findings; the runtime JAR includes unfixed `ini4j` CVE-2022-41404 (High) and OpenTelemetry CVE-2026-45292 (Medium). They are explicitly not described as fixed or as zero-CVE evidence.

## Verification

| Check | Actual result |
| --- | --- |
| Linux Java reactor, `./mvnw -B -DforkCount=2 -DargLine=-Xmx2g package` | 2,704 tests reported, 0 failures/errors; 30 existing conditional skips. All modules built. Unix Go interop runs in Linux. |
| Final SAST-related query/profile/OpenAPI/service tests, `./mvnw -B -pl skillhub-app -am -Dtest=JpaAdminNamespaceQueryRepositoryTest,UserProfileControllerTest,SkillControllerTest,AdminNamespaceAppServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` | 36 passed, including 3 new PostgreSQL query tests after the full reactor run. |
| Frontend `vitest run --maxWorkers=4` | 1,232 passed. |
| Frontend follow-up query/localization tests | 6 passed. |
| Frontend TypeScript, ESLint and production build | Passed; existing large-chunk build warnings remain. |
| Scanner existing 2.1 contract | Passed with fixture files retained and task containers stopped instead of deleting them. |
| Actual PostgreSQL / Redis / Java startup | Healthy; 119 migrations applied successfully in an isolated database. |
| Root and `/skillhub/` HTTP | Static UI, health, public search and anonymous-token rejection passed; backend OpenAPI remains 3.0.1. |
| Delivery Maven package and container build | Passed. |
| Trivy JAR and frontend scans | Critical gate passed with actual package coverage; no ignore-unfixed option. |
| `git diff --check` | Passed. |

PowerShell requires quoting dotted Maven `-D` arguments. Java verification used an isolated Linux container with Go and Docker access plus the repository fixtures. Earlier Windows Unix-Go compilation failures and incomplete harness paths are not represented as passing tests; the successful Linux run supersedes those attempts. ARM64/RISC-V validation and an organization-policy Semgrep run were not performed.

Reproduce the primary gates using `.github/workflows/critical-cves.yml`. CI scans actual built JAR and frontend lockfile packages, and runs the public Semgrep ERROR gate on Java/JS/TS production source. Container OS findings are supplementary rather than a recurring primary gate.

## Evidence

- [CVE/package scan summary](security/2026-10-28-scan-summary.json): database timestamps, artifact IDs, package coverage, before/after CVEs, remaining severities.
- [Semgrep summary](security/2026-10-28-semgrep-summary.json): rules, paths, remaining warnings, parser status and fixpoint diagnostics.
- [Test/HTTP summary](security/2026-10-28-test-summary.json): reactor totals and final HTTP results.
- [Upstream tracking state](security/upstream-state.json): exact source and releases already covered.

## Security impact

Confidentiality: only public upstream source and package metadata were used. Organization source, data, credentials and the parent Python checkout were not imported. Public rules were downloaded; Semgrep metrics and cloud result submission were not enabled. Temporary service credentials remain outside Git.

Integrity: named query parameters are preserved; authorization and audit policy are retained and covered by existing negative tests. Source history, dependency versions, scanner requirements, image digests and public CI make the reference reproducible. Upstream push is disabled; only this repository's `dev` is authorized for delivery.

Availability: Java/React builds, affected database queries, framework-dependent tests and real service health pass. JSON/OpenAPI and provider-specific OAuth compatibility are retained. No organization deployment or data migration was performed.

## Ongoing maintenance

The existing **SkillHub upstream release watch** automation remains active every 48 hours. It preserves the separate Python fork's planning-only workflow and adds release integration, best-effort Semgrep/Critical dependency remediation, relevant verification and non-force `dev` pushes for this Java security reference. Non-Critical or unrelated container issues do not expand the main task. No new schedule was created. Report meaningful updates or blockers; stay quiet without a new release or actionable change.
