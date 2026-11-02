# Progress

2026-11-02 Asia/Taipei: cloned target dev; reviewed target AGENTS, Trellis indexes, prior remediation and Semgrep/tag evidence. No application source changed. Original workspace untracked work preserved. Branch codex/semgrep-remediation-2026-11-02 created for scoped work.

2026-11-02: RED verified: domain 23 tests / 2 failures; consumer 2 tests / 1 failure against unchanged production source. Added canonical path checks at the scan boundary and strict descendant/symlink checks at consumer read/cleanup. Added downloader Unicode-host and cookie tampering/no-redirect regression coverage. Local focused Java reactor in progress. Added workflow_dispatch to existing Java and Go verification workflows to allow exact-dev Linux reruns without changing test commands or publishing artifacts.

2026-11-02: Native Java focused reactor passed (90 reported / 0 failed / 0 errors / 2 symlink skips on Windows); final domain-only rerun includes the two additional nested-backslash cases. No dependencies changed. Source gate Python tests: 10 passed. Workflow Bash-input tests: Windows temp cleanup errors, retained for Linux verification. Baseline reports from Actions run 41296194501 downloaded outside Git; only one requested-path public Semgrep warning was present (PathPresence prepared batch SQL).
