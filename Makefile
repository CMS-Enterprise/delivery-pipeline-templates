.PHONY: lint lint-yaml lint-groovy validate-policies test-policies

lint: lint-yaml lint-groovy validate-policies test-policies

lint-yaml:
	yamllint -c .yamllint.yml resources/ templates/

lint-groovy:
	npm-groovy-lint --path "vars/" --path "templates/" --files "**/*.groovy,**/Jenkinsfile"

validate-policies:
	conftest verify --policy policy/

test-policies:
	conftest test resources/pods/ --policy policy/ --namespace pods
