import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("security_gate", Path(__file__).resolve().parents[1] / "check-source-security.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SourceSecurityGateTest(unittest.TestCase):
    def report(self, severity, path="web/src/app.ts"):
        return {"paths": {"scanned": ["server/app/src/main/java/App.java", "web/src/app.ts", ".github/workflows/test.yml"]},
                "results": [{"path": path, "extra": {"severity": severity}}]}

    def test_critical_is_blocked_even_though_it_is_not_error(self):
        for severity in ("CRITICAL", "HIGH", "ERROR"):
            with self.subTest(severity=severity):
                self.assertEqual(len(module.gate(self.report(severity))["blockingFindings"]), 1)

    def test_warning_is_visible_in_report_but_does_not_block(self):
        self.assertEqual(module.gate(self.report("WARNING"))["blockingFindings"], [])

    def test_workflow_execution_findings_are_in_scope(self):
        self.assertEqual(len(module.gate(self.report("ERROR", ".github/workflows/release.yml"))["blockingFindings"]), 1)

    def test_missing_coverage_is_not_success(self):
        self.assertFalse(module.gate({"results": []})["coverage"])

    def test_production_parse_failure_is_blocked(self):
        report = self.report("WARNING")
        report["errors"] = [{"path": "web/src/app.ts", "type": "Syntax error"}]
        self.assertEqual(len(module.gate(report)["analysisErrors"]), 1)

    def test_workflow_yaml_failure_is_blocked_but_template_diagnostic_is_retained(self):
        report = self.report("WARNING")
        report["errors"] = [{"path": ".github/workflows/test.yml", "type": "Syntax error"},
                            {"path": ".github/workflows/test.yml", "type": ["PartialParsing", []]}]
        result = module.gate(report)
        self.assertEqual(len(result["analysisErrors"]), 1)
        self.assertEqual(len(result["workflowTemplateDiagnostics"]), 1)


if __name__ == "__main__":
    unittest.main()
