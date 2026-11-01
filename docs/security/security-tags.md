# Verified security update tags

For `brandonfang06/skillhub-security`, every delivered security update on `dev`
must receive a new annotated Git tag after verification. This is a standing user
request; it also applies to the existing 48-hour upstream release watcher.

- Name: `security-YYYY.MM.DD.N`, using Asia/Taipei's date and the next unused
  positive integer for that date. This namespace avoids upstream release tags
  and package-publishing triggers.
- Verify the exact commit with relevant tests, the Code and dependency security
  workflow (including both Semgrep scans and complete production-file coverage),
  and CodeQL. A completed CodeQL run is not proof that all findings are resolved;
  inspect the report for newly introduced Critical or confirmed exploitable issues.
- Confirm the commit is published on origin/dev. Record the commit, upstream base,
  Semgrep engine/rule versions, result scope and successful CI links in the tag
  annotation. Public-rule acceptance does not imply company-policy acceptance.
- Push only the named annotated tag. Verify the remote tag's peeled commit equals
  the validated commit. Never force, move, overwrite or bulk-push tags. Do not
  create a GitHub Release, publish packages or deploy as part of tagging.
- If the push succeeds but tagging fails, retry the missing tag on the next run.
  If that commit already has a security tag, keep it and do not create duplicates.
- Report the new tag and commit so downstream scans can use an immutable version.
  Incomplete scans, confirmed unresolved Critical findings or failed required
  checks must be reported rather than presented as a verified version.
