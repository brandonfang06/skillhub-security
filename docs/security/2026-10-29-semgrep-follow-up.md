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

Investigation: GitHub's code-scanning endpoint currently returns no analysis
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
