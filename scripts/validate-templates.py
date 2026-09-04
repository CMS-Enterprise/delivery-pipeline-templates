#!/usr/bin/env python3
"""Validate templates/*/template.yaml against internal/template.schema.json.

A malformed template.yaml is only discovered when CloudBees rejects the catalog,
which is the slowest possible feedback loop. The schema already existed but
nothing read it.
"""

import sys
from pathlib import Path

import yaml
from jsonschema import Draft7Validator

REPO_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = REPO_ROOT / "internal" / "template.schema.json"


def main() -> int:
    schema = yaml.safe_load(SCHEMA_PATH.read_text())
    validator = Draft7Validator(schema)

    paths = [Path(a) for a in sys.argv[1:]] or sorted(
        (REPO_ROOT / "templates").glob("*/template.yaml")
    )
    if not paths:
        print("validate-templates: no template.yaml files found.")
        return 0

    failed = 0
    for path in paths:
        rel = path.relative_to(REPO_ROOT) if path.is_absolute() else path
        try:
            doc = yaml.safe_load(path.read_text())
        except yaml.YAMLError as exc:
            print(f"FAIL  {rel}\n        unparseable YAML: {exc}")
            failed += 1
            continue

        # sorted() gives a stable report order; jsonschema yields errors in
        # dict-iteration order, which makes diffs between runs noisy.
        errors = sorted(validator.iter_errors(doc), key=lambda e: list(e.absolute_path))
        if errors:
            print(f"FAIL  {rel}")
            for err in errors:
                loc = ".".join(str(p) for p in err.absolute_path) or "(root)"
                print(f"        {loc}: {err.message}")
            failed += 1
        else:
            print(f"PASS  {rel}")

    print(f"validate-templates: {len(paths) - failed} passed, {failed} failed.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
