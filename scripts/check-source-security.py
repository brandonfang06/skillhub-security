"""Gate production Java/frontend and workflow findings from an unfiltered Semgrep report."""
import json
from pathlib import Path
import re
import sys


def primary_path(path):
    path = str(path).replace("\\", "/").removeprefix("./")
    return bool(re.match(r"server/[^/]+/src/main/java/", path)
                or path.startswith(".github/workflows/")
                or (path.startswith("web/src/") and not re.search(r"\.(test|spec)\.[jt]sx?$", path)))


def gate(report):
    scanned = report.get("paths", {}).get("scanned", [])
    coverage = all(any(str(path).startswith(prefix) for path in scanned)
                   for prefix in ("server/", "web/src/", ".github/workflows/"))
    findings = []
    for finding in report.get("results", []):
        if not primary_path(finding.get("path", "")):
            continue
        extra = finding.get("extra", {})
        if str(extra.get("severity", "")).upper() in {"ERROR", "HIGH", "CRITICAL"}:
            findings.append({"rule": finding.get("check_id"), "path": finding.get("path"),
                             "line": finding.get("start", {}).get("line"), "severity": extra.get("severity")})
    errors = []
    template_diagnostics = []
    for error in report.get("errors", []):
        path = str(error.get("path", ""))
        error_type = error.get("type")
        # GitHub expands expressions before Bash runs; retain embedded-shell
        # parser diagnostics separately from YAML and application parse failures.
        if path.startswith(".github/workflows/") and isinstance(error_type, list) and error_type[0] == "PartialParsing":
            template_diagnostics.append(error)
        elif not path or primary_path(path):
            errors.append(error)
    return {"coverage": coverage, "scannedFiles": len(scanned),
            "blockingFindings": findings, "analysisErrors": errors,
            "workflowTemplateDiagnostics": template_diagnostics}


if __name__ == "__main__":
    result = gate(json.loads(Path(sys.argv[1]).read_text(encoding="utf-8")))
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["coverage"] and not result["blockingFindings"] and not result["analysisErrors"] else 1)
