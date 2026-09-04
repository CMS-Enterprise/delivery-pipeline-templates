.PHONY: init lint lint-yaml validate-policies test-policies lint-secrets

# Per-clone setup. Both steps write to local git state, which cannot be
# committed, so a fresh clone has neither until this runs.
init:
	prek install -t pre-commit -t commit-msg -t pre-push
	git config commit.template .gitmessage

lint: lint-yaml validate-policies test-policies

lint-yaml:
	yamllint -c .yamllint.yml resources/ templates/

validate-policies:
	conftest verify --policy policy/

test-policies:
	conftest test resources/pods/ --policy policy/ --namespace pods

# Full-history scan, ~10s over 631 commits. Deliberately outside `lint` and
# outside every hook; the pre-push hook scans only the range being pushed.
lint-secrets:
	gitleaks git --redact --no-banner .
