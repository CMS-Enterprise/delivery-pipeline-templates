.PHONY: init lint lint-yaml validate-policies test-policies validate-templates \
	lint-jenkinsfiles lint-secrets

# Per-clone setup. Both steps write to local git state, which cannot be
# committed, so a fresh clone has neither until this runs.
init:
	prek install -t pre-commit -t commit-msg -t pre-push
	git config commit.template .gitmessage

lint: lint-yaml validate-policies test-policies validate-templates

lint-yaml:
	yamllint -c .yamllint.yml resources/ templates/

validate-policies:
	scripts/run-tool.sh conftest verify --policy policy/

test-policies:
	scripts/run-tool.sh conftest test resources/pods/ --policy policy/ --namespace pods

validate-templates:
	python3 scripts/validate-templates.py

# 3.8s for the whole tree, which fits neither the 0.65s pre-commit nor the 2.1s
# pre-push budget, so the full run stays on demand. The pre-push hook validates
# only changed files, at 0.22s each.
lint-jenkinsfiles:
	./scripts/jenkins-lint.sh

# Full-history scan, ~10s over 631 commits. Deliberately outside `lint` and
# outside every hook; the pre-push hook scans only the range being pushed.
lint-secrets:
	scripts/run-tool.sh gitleaks git --redact --no-banner .
