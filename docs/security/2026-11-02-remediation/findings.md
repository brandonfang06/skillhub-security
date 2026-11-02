# Findings

- Fresh independent checkout of target dev is clean; existing workspace is a different Python-only repository and is left untouched.
- Existing target uses Java with Spring Boot 4 security hardening and contains prior Semgrep evidence. GitHub Code Scanning API currently returns CodeQL alerts, not the original approximately 31 organization Semgrep findings.
- User kit not yet found in Downloads, Documents, projects, OneDrive, iCloudDrive or local Temp. Requested its path/link while continuing source review; it has not been executed.
- Prior public Semgrep evidence explicitly did not reproduce organization rules. The original set must remain unverified unless recovered.
- Native Java/Maven/Semgrep absent from PATH. Go, Python, uv, gh and Docker CLI available; Docker engine initially stopped.

## Confirmed feedback and decisions

- Added negative tests before fixes. On unchanged source, SecurityScanServiceTest ran 23 tests and failed both local/upload noncanonical path cases (nested/../escape.txt). Consumer path test also runs against unchanged source in java-consumer-red.log.
- Use the existing SkillPackagePolicy at the scan boundary before any file writes, audit writes or task publication. It already normalizes separators and rejects traversal/drive prefixes. No generated files, query semantics, dependency versions or suppressions change.
- Validate queue-supplied local scan paths as strict descendants of the scan workspace and reject existing symbolic-link components. Apply the same guard to cleanup, including rejection of the workspace root.
- Confidentiality: scanner must not read arbitrary host paths from task input. Integrity/availability: cleanup must not delete the shared workspace or follow a link outside it. Existing authorization, audit sequencing and attempt locking remain intact.
- Local Docker startup failed in its inference-manager socket initialization; no factory reset, data deletion or machine-wide repair attempted. Windows Go tests cannot compile the Linux syscall implementation. Use existing GitHub Linux PR checks for real PostgreSQL/Docker/Go coverage.

## Residual assumptions

The fixed scan workspace remains service-owned. Existing-symlink checks prevent the reproduced link escapes; they are not a race-free filesystem sandbox against a privileged local process replacing components concurrently. The original ~31 findings, organization engine/rules and remediation-kit contents remain unavailable. Public scans cannot establish their closure.
