"""Gate production Java/frontend and workflow findings from an unfiltered Semgrep report."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys


def normalized_path(path):
    return str(path).replace("\\", "/").removeprefix("./")


def primary_path(path):
    path = normalized_path(path)
    return bool(re.match(r"server/[^/]+/src/main/java/", path)
                or path.startswith(".github/workflows/")
                or (path.startswith("web/src/") and not re.search(r"\.(test|spec)\.[jt]sx?$", path)))


def tracked_primary_sources():
    tracked = subprocess.check_output(["git", "ls-files", "-z"], text=True).split("\0")
    return [path for path in tracked if primary_path(path)
            and Path(path).suffix in {".java", ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs", ".yml", ".yaml"}]


def gate(report, expected_paths=None, java_only=False):
    scanned = [normalized_path(path) for path in report.get("paths", {}).get("scanned", [])]
    expected = {normalized_path(path) for path in expected_paths} if expected_paths is not None else set()
    missing = sorted(expected - set(scanned))
    prefixes = ("server/",) if java_only else ("server/", "web/src/", ".github/workflows/")
    coverage = all(any(path.startswith(prefix) for path in scanned) for prefix in prefixes) and not missing
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
            "expectedPrimaryFiles": len(expected), "missingFiles": missing,
            "blockingFindings": findings, "analysisErrors": errors,
            "workflowTemplateDiagnostics": template_diagnostics}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("--java-only", action="store_true")
    args = parser.parse_args()
    expected = tracked_primary_sources()
    if args.java_only:
        expected = [path for path in expected if path.startswith("server/") and path.endswith(".java")]
    result = gate(json.loads(args.report.read_text(encoding="utf-8")),
                  expected_paths=expected, java_only=args.java_only)
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["coverage"] and not result["blockingFindings"] and not result["analysisErrors"] else 1)
