# Progress

2026-11-02 Asia/Taipei: cloned target dev; reviewed target AGENTS, Trellis indexes, prior remediation and Semgrep/tag evidence. No application source changed. Original workspace untracked work preserved. Branch codex/semgrep-remediation-2026-11-02 created for scoped work.

2026-11-02: RED verified: domain 23 tests / 2 failures; consumer 2 tests / 1 failure against unchanged production source. Added canonical path checks at the scan boundary and strict descendant/symlink checks at consumer read/cleanup. Added downloader Unicode-host and cookie tampering/no-redirect regression coverage. Local focused Java reactor in progress. Added workflow_dispatch to existing Java and Go verification workflows to allow exact-dev Linux reruns without changing test commands or publishing artifacts.

2026-11-02: Native Java focused reactor passed (90 reported / 0 failed / 0 errors / 2 symlink skips on Windows); final domain-only rerun includes the two additional nested-backslash cases. No dependencies changed. Source gate Python tests: 10 passed. Workflow Bash-input tests: Windows temp cleanup errors, retained for Linux verification. Baseline reports from Actions run 41296194501 downloaded outside Git; only one requested-path public Semgrep warning was present (PathPresence prepared batch SQL).

2026-11-02: PR #1 merged into dev as 0a2afafccefc2144f0e74c82fad3ba3b8099ddd1 after both complete Java runs passed, Go/vet/images, Semgrep/actual Critical package gates, CodeQL, frontend and E2E passed. Dependency Review alone is unsupported by repository configuration; failure is preserved, not bypassed by changing its workflow. New report JSON records all evidence and unknown original findings.

Additional execution notes: connector PR creation returned403 and gh authenticated fallback succeeded. A plan EOF whitespace defect was corrected in a follow-up commit before merge. Local summary helpers initially encountered Windows cp950 decoding and SARIF extension-rule lookup errors; UTF-8 and full rule catalogs resolved them. No source/testing gate was weakened.
