"""Exercise workflow shell steps with hostile dispatch values (requires PyYAML and bash)."""
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[2]


class WorkflowShellInputsTest(unittest.TestCase):
    def test_dispatch_values_cannot_execute_shell_code(self):
        cases = [
            ("issue-backlog-rescore.yml", "Rescore deferred issues", "inputs.limit"),
            ("issue-triage.yml", "Run triage", "inputs.issue_number"),
            ("pr-batch-test-deploy.yml", "Merge selected PRs onto base ref", "inputs.pr_numbers"),
            ("publish-chart.yml", "Parse version from tag", "inputs.version"),
            ("release-cli.yml", "Validate tag (workflow_dispatch only)", "github.event.inputs.tag"),
            ("release-cli.yml", "Extract version from tag", "github.event.inputs.tag"),
            ("release-cli.yml", "Create GitHub Release", "github.event.inputs.tag"),
            ("release-notes.yml", "Generate release notes", "github.event.inputs.tag"),
        ]
        for filename, name, field in cases:
            with self.subTest(workflow=filename, step=name), tempfile.TemporaryDirectory() as directory:
                work = Path(directory)
                marker = work / "injection-ran"
                payload = f"$(touch '{marker.as_posix()}')"
                document = yaml.safe_load((ROOT / ".github/workflows" / filename).read_text(encoding="utf-8"))
                step = next(step for job in document["jobs"].values() for step in job.get("steps", [])
                            if step.get("name") == name)
                values = {field: payload, "github.event_name": "workflow_dispatch",
                          "github.repository_owner": "owner", "github.event.repository.name": "repo",
                          "github.ref_name": "v1.2.3", "needs.build-and-test.outputs.version": "1.2.3",
                          "needs.build-and-test.outputs.package_name": "skillhub"}

                def expand(text):
                    def expression(match):
                        for item in match.group(1).split("||"):
                            item = item.strip()
                            value = item[1:-1] if item.startswith("'") else values.get(item, "")
                            if value:
                                return value
                        return ""
                    return re.sub(r"\$\{\{\s*(.*?)\s*\}\}", expression, str(text))

                env = {**os.environ, "GITHUB_OUTPUT": str(work / "output"),
                       "GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_REPOSITORY_OWNER": "owner",
                       "GITHUB_REF_NAME": "v1.2.3",
                       "GITHUB_REPOSITORY": "owner/repo"}
                env.update({key: expand(value) for key, value in step.get("env", {}).items()})
                # Stub external operations; execute the actual workflow shell text unchanged.
                stubs = "deno() { return 0; }; git() { return 1; }; gh() { return 0; }; export -f deno git gh\n"
                result = subprocess.run([os.environ.get("WORKFLOW_TEST_BASH", "bash"), "-c",
                                         stubs + expand(step["run"])], cwd=work,
                                        env=env, capture_output=True, text=True, timeout=10)
                self.assertFalse(marker.exists(), json.dumps({"workflow": filename, "step": name,
                                                              "exit": result.returncode}))


if __name__ == "__main__":
    unittest.main()
