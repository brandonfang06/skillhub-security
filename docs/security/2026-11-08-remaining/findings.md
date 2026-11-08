# Investigation notes

## Baseline
- Clean local dev and fetched origin/dev both dadacdd4b18f97c2e48e90dd36f1e68ede4a4166.
- Working branch codex/security-remaining-cwes-2026-11-08.
- Supplied attachment is a complete mission inventory, not a raw Semgrep report. Twelve explicit line groups map to SkillWebUrls (1), ScanTaskConsumer cleanup (1), Upload repository (1), SystemCredential repository (3), DiscoveryEntry repository (5), InitialAuthSettingsInitializer (1).
- SkillWebUrls, SystemCredential and DiscoveryEntry are newly explicit compared with the prior patch. Current credential/discovery SQL concatenates compile-time column lists; caller values are parameterized. Exploitability must be evaluated separately from the original unknown rule trace.
- Earlier delivery reports 14 production Java and two Go fixture changes; no original 31-finding closure claim was made.

## Reproduced behavior and decisions
- Native Java reproduced SkillWebUrls NFC conversion. Added regression failed (expected cafe%CC%81, actual caf%C3%A9); replaced implicit canonicalization with UTF-8 encoding of non-ASCII runs while preserving existing ASCII/percent escapes. Reject malformed UTF-16 deployment configuration and retain the URI host/user-info/query/fragment checks.
- Same-version local scans used the identical directory. A public triggerScan regression failed before the change. Each attempt now uses a new provider-created temporary directory; pre-existing per-version directory symlinks are never reused.
- Duplicate portable entry names were not rejected before streaming. Both local/upload negative regressions failed before the duplicate-name set was added; no content, audit, outbox/task or version write may occur for such input.
- Cleanup uses descriptor-relative NOFOLLOW_LINKS traversal on providers with SecureDirectoryStream (Linux CI). The existing guarded pathname fallback is retained for unsupported providers such as Windows. It still requires a service-owned workspace and is not a race-free sandbox there. This is a material limitation, not an unconditional cross-platform race-free claim.
- Current-source SQL for all ten reported SQL groups is constant/bound; do not rewrite safe constants just to eliminate an unknown audit match. Added isolated PostgreSQL API tests for hostile quoted data and namespace isolation.
- Fresh baseline workflow 42473139942 on exact dadacdd4 succeeded: eight public packs 199 matches, official Java four; zero matches in the six remaining-site production files before this patch; actual JAR/frontend Critical CVEs zero (347/737 package entries). These public rules cannot reproduce the supplied company's 12 alerts.
- Added a deterministic Linux completion race regression at the filesystem boundary: replace a validated parent with an external-directory symlink immediately before the first pathname/handle I/O, then assert the outside sentinel survives. Actual directory moves/links and consumer completion are used; no private method is invoked. Linux CI must execute this case; no pre-fix Linux test pass/failure claim is made before that execution.
- Run the scan workspace regression class first in backend-tests so filesystem/mock-maker incompatibilities fail before the long database suite. The full reactor remains mandatory; the early step is not a substitute for integration testing.
