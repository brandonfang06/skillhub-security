# Semgrep verification and security tags — 2026-11-01

The user still sees internal Semgrep findings and requests independent Semgrep
verification plus a new tag after every verified security update. Internal rules
and findings cannot be exported; do not ask for them again or equate CE results
with organization acceptance.

Plan and verifiable criteria:

1. Scan exact dev source with seven public packs and the official Java rule tree;
   retain all severities and inline-suppressed findings, compare actual scanned
   paths against every tracked production Java/frontend source file, and report
   analysis diagnostics. Confirm concrete defects before changing application code.
2. Strengthen coverage checks with regression tests if current checks miss files.
   Run the actual scanner and CI on the published commit; preserve remaining
   warnings and limitations rather than hiding them.
3. After verification, create an annotated `security-YYYY.MM.DD.N` tag pointing
   to the verified dev commit. Check remote tag and peeled commit. Never move or
   overwrite an existing tag, and never push all inherited upstream tags.
4. Amend the existing 48-hour upstream watcher to tag each verified update using
   the same convention, including recovery of a missing tag after a prior push.

Starting commit: `51cf87d0` (same as origin/dev at investigation start).
The existing workflow's fresh run is 41267940789.

## Executed checks

The seven-pack Semgrep rerun on `51cf87d0` passed in
[run 41267940789](https://github.com/brandonfang06/skillhub-security/actions/runs/41267940789).
All 1,605 expected production Java/frontend source and workflow files were
present in the scanned-path inventory, including all 1,283 production Java files.
The full report has 187 findings; primary-scope ERROR/HIGH/CRITICAL and application
analysis errors are zero. Three Java production WARNING findings remain.

A second independent Semgrep CE 1.181.1 scan used the entire official `java/`
rule tree at semgrep/semgrep-rules commit
`16b9e6d45f10649f6a531ec4838ed9924372c3bc`. It ran 123 applicable rules against
1,429 backend files, including all 1,283 production Java files. Result: three
WARNING, two INFO, zero ERROR/HIGH/CRITICAL, zero reported analysis errors.
The scanner log reports approximately 100% parsed lines; 11 ignored paths remain
outside the verified production-Java set. No source suppressions were enabled.

The five results were reviewed:

| Result | Location | Context |
| --- | --- | --- |
| SHA-1 (WARNING) | GithubSyncArchiveValidator.java:396 | Git blob object checksum; package fingerprint uses SHA-256 separately. |
| MD5 (WARNING) | LegacyOAuthIdentityCoordinate.java:42 | Existing deterministic provider database identifier, not a password/signature. Changing it requires compatibility migration. |
| SQL injection audit (WARNING) | JdbcGithubSyncPathPresenceRepository.java:80 | Constant SQL text; inputs use PreparedStatement setters. No SQL injection demonstrated. |
| GCM audit (two INFO) | AesGcmGithubCredentialCipher.java:116-117 | Encryption generates a fresh SecureRandom nonce; credential ID and key version are authenticated as AAD. No nonce reuse demonstrated. |

No new confirmed application vulnerability was reproduced in these Semgrep runs.
This update strengthens the scan gate: every tracked primary source file must
appear in the scanner inventory. Previously one scanned file per directory was
enough for the coverage check. Four new regression tests first failed against
the old gate; all ten gate tests now pass. Eight existing hostile workflow-input
scenarios also pass. The complete official Java rule-tree scan is now retained
in CI alongside the seven public packs. No application source changed, so the
previous backend regression results are not represented as newly executed tests.

## Reproduction and limitations

From the repository root, with the pinned scanner image in the workflow:

```sh
semgrep scan --oss-only --config p/default --config p/owasp-top-ten --config p/security-audit --config p/java --config p/javascript --config p/typescript --config p/secrets --metrics=off --disable-nosem --no-git-ignore --timeout 30 --json --output semgrep.json .
python scripts/check-source-security.py semgrep.json
# Check out the pinned official rule repository into .security-rules first.
semgrep scan --oss-only --config .security-rules/java --metrics=off --disable-nosem --no-git-ignore --max-target-bytes 0 --timeout 60 --json --output semgrep-java.json server
python scripts/check-source-security.py --java-only semgrep-java.json
```

Raw reports are uploaded as `semgrep-source` workflow artifacts. The compact
`2026-11-01-semgrep-evidence.json` includes hashes and the five backend matches.
The full repository still contains non-primary findings, and the seven-pack
report retains 36 workflow-template parsing diagnostics.

This is actual Semgrep verification, but it uses the Community engine and public
rules. [Semgrep documents](https://docs.semgrep.dev/semgrep-code/semgrep-pro-engine-intro)
that CE analyzes within a function, while Semgrep Code supports cross-function
and cross-file analysis. Unknown company rules, engine, severity policy and
scanned revision have not been reproduced. The reported internal findings remain
unverified; no claim of company approval or Critical-zero under its policy is made.
[Semgrep ignore behavior](https://docs.semgrep.dev/ignoring-files-folders-code)
also motivates the explicit per-file coverage check.

The existing 48-hour automation is updated to create a new annotated tag after
every verified dev delivery, following `security-tags.md`. The intended first tag
is `security-2026.11.01.1`; creation is contingent on this commit's CI results.
