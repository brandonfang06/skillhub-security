# Semgrep remediation plan — 2026-11-02

Target: brandonfang06/skillhub-security dev, starting at 8c9c085659a412f61f64621e988b1c1fdd017e89. Only this Java/Go security reference may be delivered; preserve the separate Python repository.

1. [audited with unavailable kit/original findings explicitly recorded] Locate and review the user remediation kit README/scripts; identify original findings, public scanner evidence and actual source. Verify: inventory and per-CWE evidence.
2. [complete: local Java passes; Linux symlink verification pending] Add failing regression tests and surgical fixes for confirmed issues. Verify: targeted Java/Go tests, real dependency checks where required; retain Spring Boot 4 and existing Critical CVE fixes.
3. [in progress] Run Java/Go regression and Semgrep with explicit versions/rules/coverage/diagnostics. Verify: distinguish confirmed fixes, audit findings, unavailable original results and remaining findings.
4. [pending] Review complete diff, commit and integrate into dev without force; verify exact published commit CI before annotated security-semgrep-2026-11-02 tag. Verify remote branch and peeled tag SHA.

Do not run unreviewed remediation scripts, add suppressions, modify generated files, overwrite existing tags or push upstream. Missing original organization findings do not imply acceptance.
