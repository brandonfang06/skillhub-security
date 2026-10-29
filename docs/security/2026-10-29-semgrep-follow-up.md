# Semgrep acceptance follow-up — 2026-10-29

The user reports 31 Critical Semgrep findings after delivery. Organization
Semgrep acceptance is therefore unresolved; the previous public-rule and
dependency results do not establish that these 31 findings are fixed.

Published baseline: `5f10ff9e323b56d4175929687e2840b59308df30` on
`brandonfang06/skillhub-security/dev`.

The delivered source gate used Semgrep OSS 1.181.1 and public `p/java`,
`p/javascript`, `p/typescript`, `p/security-audit`, filtered to ERROR severity,
on production Java and frontend source. It did not reproduce the organization's
policy. The unfiltered local public-rule report retained four WARNING findings
and documented taint fixpoint diagnostic limits. Dependency CVE results and
source findings are separate acceptance evidence.

At the start of this investigation, GitHub's code-scanning endpoint returned no analysis
for this repository. Existing local scan artifacts and Semgrep/SARIF-named
Downloads files do not contain the reported 31-result export. No specific cause,
false-positive classification, or resolution is established.

The user cannot export organization reports and explicitly requests independent
discovery. Proceed without waiting for organization data. Expand public Semgrep
rule coverage and scan tracked source beyond the original production directories;
inspect security-sensitive Java/JS data flows and validate concrete defects.

Plan and acceptance:

1. Run expanded public-rule scans without a severity filter or inline suppression;
   inventory source, rules, findings and skipped/failed analysis.
2. Trace high-risk findings through real HTTP, archive, database and rendering
   boundaries. Reproduce defects with the previously approved Java/frontend test
   seams, then implement focused fixes and rerun those tests.
3. Rerun scans and relevant regression checks, record exact evidence, then commit
   and push verified fixes to dev. Do not equate independent findings with the
   unavailable set of 31 or claim organization-policy acceptance.

Confidentiality and integrity focus: untrusted package content, URL destinations,
HTML/SVG rendering, file access, queries and credentials. Availability checks cover
bounded inputs and analysis completeness. No organization data is needed or used.

## Independently reproduced findings and changes

- Expanded Semgrep to seven public packs, all severities and tracked repository
  source, with inline suppressions disabled: 802 applicable rules / 2,905 files
  initially produced 194 findings. Nine ERROR findings were in production
  Java/frontend or workflows. Eight workflow dispatch interpolations executed
  a harmless marker command in a local adversarial test; environment variables
  and quoted arguments now prevent that execution. This requires access to the
  affected dispatch/trigger input; no unauthenticated exploitation is claimed.
- The remaining Java ERROR matched a dummy BCrypt hash, not an account credential.
  A real encoder-compatibility regression was reproduced: missing-user login
  threw an encoder error with DelegatingPasswordEncoder. The dummy hash now uses
  the configured encoder and random input once per service instance. Missing
  users still receive Unauthorized. Auth regression: 29 tests pass.
- Code previews now render highlight AST nodes with React text escaping. Mermaid
  already uses strict mode and internal sanitization; an independent DOMPurify
  boundary now cleans SVG/foreignObject output before DOM insertion. These are
  defense-in-depth changes, not independently proven Critical XSS exploits.
  Rendering tests: 16 pass, including hostile markup and preserved diagram labels.
- Added a gate for ERROR/HIGH/CRITICAL, required path coverage and production parse
  failures using the unfiltered report. Six gate tests and eight workflow attack
  cases pass. Embedded workflow Bash parsing of GitHub template expressions is
  retained as a separate diagnostic, while malformed YAML still fails.

Second public-rule run: 802 applicable rules / 2,908 files, 187 total findings;
zero blocking findings or application parse errors in the primary scope. The
single remaining repository-wide ERROR is an HTTP request in the separate DSH
host probe. Warnings and default-ignore/large-file exclusions remain visible.

FindSecBugs 1.14.0 / SpotBugs 4.9.3 analyzed compiled project classes with all
runtime dependencies: 651 findings including informational Spring endpoint
inventory, zero analysis errors and zero missing classes. Three high-priority
findings concern legacy MD5 identifiers, stateless worker CSRF configuration and
path normalization; these require contextual review and are not classified here
as confirmed Critical exploits. Added CodeQL security-extended for Java and JS/TS
cross-file analysis; its findings must be reviewed separately when the run ends.

The user clarified that the inaccessible 31 findings are believed to be mainly
backend issues. Java source data-flow review and CodeQL are the current priority.
Neither the counts above nor tool severity names establish equivalence to those
31 findings or prove that organization-policy acceptance has passed.

## Backend data-flow follow-up

CodeQL security-extended at `5a28735e1e0f0e84842ef273a79239265f6d9ace`
completed successfully for both languages ([run 40896403369](https://github.com/brandonfang06/skillhub-security/actions/runs/40896403369)).
Java produced 53 findings; JavaScript produced 17 (none under `web/src`).
No result had a CodeQL security-severity of 9.0 or greater. This is CodeQL's
classification, not evidence about the organization's Semgrep policy.

Concrete backend defects reproduced before changes:

- Large page numbers overflowed integer arithmetic in audit-log SQL offsets,
  filtered skill-list slicing, governance inbox aggregation and CLI next cursors.
  Offsets and slice arithmetic now use long intermediates; the CLI stops at the
  largest representable page. Governance inbox rejects invalid page sizes and
  requested prefixes above 10,000 items before querying its three sources, to
  bound memory/query amplification. This introduces an intentional HTTP 400 for
  `(page + 1) * size > 10,000`; each page remains limited to 100 items.
- Failed staging cleanup wrote a preview capability token and the storage
  exception to logs. It now records only the exception type; terminal-operation
  cleanup also omits raw exception contents. Persisted retry/failure evidence is
  retained. No authentication policy, database schema or cleanup scope changed.

The added adversarial cases produced eight failed assertions and one error
before fixes (51 total tests), including negative indexes/cursors and a captured
preview token in the logger. These are confirmed defects; CodeQL classified the
associated arithmetic/log findings as High, not Critical.

Other Java findings were traced through their context: the disabled-CSRF chain
requires a dedicated Bearer service token on every request and is stateless;
legacy MD5 is a compatibility identifier, and SHA-1 is a Git protocol checksum.
The four user-controlled-bypass reports cover explicit authentication/session
checks, not independently demonstrated bypasses. The 16 command-path reports
are in test harnesses. SQL strings flagged by FindSecBugs use constant clauses
and bound values; no injection was demonstrated. These findings are retained,
not hidden through inline suppressions or renamed APIs. Lower-severity log and
lock findings still require normal security maintenance.

Verification commands (run from the stated directory):

```text
# server (Java 21 Maven container)
./mvnw -B -pl skillhub-app -am -Dtest=MySkillAppServiceTest,AdminAuditLogAppServiceTest,GovernanceWorkbenchAppServiceTest,CliSkillAppServiceTest,SkillSuiteBundleStagedCleanupServiceTest,GovernanceControllerTest,CliSkillControllerTest,AuditLogControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -pl skillhub-app -am -Dtest=*Security*,*Authorization*,*TokenScope*,OAuthLoginRedirectSupportTest,SkillMetadataParserTest,SkillPackageArchiveExtractorTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=2 -DargLine=-Xmx2g test
# repository root
python scripts/tests/source-security-gate-test.py
python scripts/tests/workflow-shell-inputs-test.py
python scripts/check-source-security.py .tmp/semgrep-expanded-backend-final.json
# web
node node_modules/vitest/vitest.mjs run --maxWorkers=4
node node_modules/typescript/bin/tsc -b
node node_modules/eslint/bin/eslint.js . --report-unused-disable-directives --max-warnings 0
node node_modules/vite/bin/vite.js build
```

Focused backend verification: 76 tests pass, including HTTP 400 for an oversized
inbox window and acceptance of the last allowed page. Security regression: 242
tests, zero failures/errors/skips. Frontend: 1,237 tests
pass, typecheck/lint/production build pass. Gate tests: six pass; actual workflow
shell-injection scenarios: eight pass. Post-backend Semgrep scan: 2,908 files,
185 total findings (178 WARNING, five INFO, one MEDIUM, one ERROR); primary-scope
blocking findings and application analysis errors are zero. The remaining ERROR
is still the separate DSH HTTP probe. The report retains 36 workflow-template
parser diagnostics; default file exclusions and non-primary diagnostics remain.
The successful dependency/source CI run at the preceding commit is
[40896403022](https://github.com/brandonfang06/skillhub-security/actions/runs/40896403022).
The backend commit triggers fresh CodeQL and dependency/source checks; their
artifacts are available in this repository's Actions tab.

Organization acceptance remains unresolved. No company report was requested
again or transferred. The existing 48-hour release watch now includes this
expanded source-analysis baseline and the unresolved 31-finding context.
