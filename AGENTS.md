All artifacts and containers are scanned with clamav before use.
freshclam is run pointing to artifactory.cloud.cms.gov for its cache

cosign is checked for containers

All code is linted
All code is scanned with trivy, sonarqube and snyk.
these scans run in parallel
trivy is the vulnerability scanner, not grype

unit tests are run, depending on the language and test type eg junit, cucumber

Containers are built with podman and signed with cosign
flag for reproducable builds

the squid proxy that only allows artifactory.cloud.cms.gov belongs to the
ironbank / batcave builds, not the standard pipeline. see templates/ironbankish/
and ironbankbuild.md. the standard pod-pipeline does not proxy its build egress.

containers are scanned with Jfrog Xray and snyk
SBOMs are created with jfrog xray and with snyk
sbom is scanned and kept as a build artifact
containers are scanned for policy violations with OpenSCAP

selenium test are run, via selenium box installation on selenium.cloud.cms.gov
same with playwright
jmeter test are run
a container can be scanned with zap

use cloudformation stack to stand up DLTA
and one to tear it down .

terraform is used to stand up an environment
terraform is used to tear down an environment
they use tfenv to allow for the code to specify the version, but scan tfenv and terraform binaries before use

all downloads have static hashes which are updated by an update script as well as renovate
all credentials have a rotate script which connects to each service to rotate them and save the new api key or other credential as a jenkins credential

cloudformation is used to stand up an environment
cloudformation is used to tear down an environment

anything using aws, including terraform and cloudformation, needs to run the awsAssumeRole.groovy in order to obtain the correct aws credentials

kubectl is used to update a service
fluxcd is used to update a service
argocd is used to update a service
these all use gitops methodology

all deployments have a timeout and rollbak method to detect and correct failure

ansible is used to update a service
ansible can be used with aws ssm and therefore also need awsAssumeRole
