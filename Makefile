.PHONY: lint lint-yaml validate-policies test-policies lint-secrets

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
