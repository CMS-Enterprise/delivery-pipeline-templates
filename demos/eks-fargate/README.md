# Demos on EKS Fargate

Terraform for an EKS cluster that runs only on Fargate, plus the Kubernetes
manifests that run the seven demo services (django, go, maven, gradle, nextjs,
sveltekit, vue) on it. Terraform builds the cluster. Flux deploys the workloads
from the `delivery-pipeline-manifests` repository, so the pipeline changes what
runs only by committing to that repository.

**_Untested: nothing here has been planned or applied yet._** See
[Not yet verified](#not-yet-verified).

```
terraform/environments/dev/   cluster, IAM, logging (Terraform)
  policies/                     AWS Load Balancer Controller IAM policy (upstream v3.6.3)
manifests/                    seed content for the delivery-pipeline-manifests repo root
  base/                         Deployment, PDB, Service, Ingress per service
  overlays/dev/                 namespace, images (pipeline-maintained), hostnames
  clusters/dev/                 Flux Kustomization, Fargate logging config
```

## What Terraform creates

| Resource | Notes |
| --- | --- |
| EKS cluster `<name_prefix>-<environment>` | Private API endpoint (public optional), access-entry auth (`API` mode), KMS envelope encryption of Secrets, all five control plane log types, standard support only |
| Cluster IAM role | `AmazonEKSClusterPolicy` |
| Fargate profiles | `system`: `kube-system` and `flux-system`; `apps`: `var.app_namespaces` (default `delivery-demos-dev`). Created one after the other, as EKS requires |
| Fargate pod execution role | `AmazonEKSFargatePodExecutionRolePolicy`, plus write access to the Fargate log group |
| CoreDNS managed add-on | `computeType: Fargate`, so DNS runs on Fargate. It replaces the bootstrapped EC2-only CoreDNS |
| IAM OIDC provider | For IRSA. Fargate pods cannot use EKS Pod Identity |
| AWS Load Balancer Controller IAM role and policy | IRSA role bound to `kube-system/aws-load-balancer-controller` |
| CloudWatch log groups | `/aws/eks/<cluster>/cluster` and `/aws/eks/<cluster>/fargate`, KMS-encrypted, `log_retention_days` |
| KMS key and alias | Encrypts Secrets and both log groups; rotation on |
| Subnet tags (optional) | `kubernetes.io/role/internal-elb=1` when `tag_subnets_for_internal_alb` is true |

Networking is not created. As in most CMS accounts, the VPC belongs to the
account, so the cluster takes an existing VPC and private subnets as input.
The subnets need a route to `artifactory.cloud.cms.gov`, because every image,
including the controllers', comes from there.

Terraform does not use the Kubernetes or Helm providers. Anything inside the
cluster goes through gitops, and the only provider is `hashicorp/aws`, under
the same `~> 6.0` constraint as `demos/terraform`. The committed
`.terraform.lock.hcl` is a copy of the one in `demos/terraform` (aws 6.72.0).

### Variables

| Name | Default | Description |
| --- | --- | --- |
| `environment` | (required) | `dev` in `dev.tfvars` |
| `vpc_id` | (required) | Existing VPC |
| `private_subnet_ids` | (required) | At least two private subnets in different AZs |
| `region` | `us-east-1` | |
| `name_prefix` | `robocare-demo` | Cluster is `<name_prefix>-<environment>` |
| `kubernetes_version` | `1.35` | EKS minor version |
| `tag_subnets_for_internal_alb` | `false` | Tag the subnets for ALB discovery. Leave false if they are already tagged, because destroy removes tags that Terraform added |
| `endpoint_public_access` | `false` | Also expose a public API endpoint |
| `public_access_cidrs` | `[]` | Allowed CIDRs for the public endpoint |
| `admin_principal_arns` | `[]` | Extra cluster admins (access entries). The principal that runs apply is admin already |
| `app_namespaces` | `["delivery-demos-dev"]` | Fargate selectors for workloads (1 to 5) |
| `coredns_addon_version` | `null` | Exact add-on version; `null` takes the EKS default at create time |
| `log_retention_days` | `90` | CloudWatch retention |

### Outputs

`cluster_name`, `cluster_endpoint`, `cluster_certificate_authority_data`,
`cluster_security_group_id`, `vpc_id`, `oidc_provider_arn`,
`lb_controller_role_arn`, `fargate_pod_execution_role_arn`,
`fargate_log_group_name`, `kms_key_arn`.

## Running it from the pipeline

This works the same way as `demos/terraform`. Run `awsAssumeRole` first, then
`terraformDeploy`, which runs in the pinned IronBank `ironbank/hashicorp/terraform`
image from `resources/pods/terraform.yaml`. That image's 1.15 satisfies
`required_version = "~> 1.15.0"`. `terraform init -lockfile=readonly` installs
the provider against the committed hashes, and ClamAV scans `.terraform/providers`
before plan.

```groovy
def eksConfig = awsConfig + [
    terraform_dir: 'demos/eks-fargate/terraform/environments/dev',
    provider_mirror: env.TF_PROVIDER_MIRROR ?: '',
]

if (!env.AWS_SESSION_TOKEN) {
    awsAssumeRole(eksConfig, 'dev')
}
terraformDeploy(eksConfig + [auto_approve: false, unstash: 'workspace',
                             stage: 'Terraform: plan eks-fargate dev'], 'dev')
```

The assumed role (`delivery-demos-terraform`) needs EKS, IAM (roles, policies,
OIDC providers), KMS, CloudWatch Logs and `ec2:CreateTags`/`DeleteTags`
permissions. It becomes the cluster's first admin.

Before the first plan:

1. Set `vpc_id` and `private_subnet_ids` in `dev.tfvars`.
2. Add a remote state backend. Like `demos/terraform`, this has none yet, and
   local state in an ephemeral pod is lost after apply. Keep `auto_approve: false`
   until a backend exists.

Rollback for the infrastructure itself: revert the commit and re-run the
plan/apply. Each EKS resource has an explicit `timeouts` block, so a stuck
create or update fails the stage instead of hanging.

## Bootstrapping the cluster (one-time, manual)

Terraform stops at the AWS boundary. Installing in-cluster controllers from
Terraform would need the Helm or Kubernetes provider, which means new lock file
hashes and a Terraform-owned path into the cluster that bypasses gitops. These
steps run once, as a cluster admin, after the first apply:

1. **Flux.** The repository already references both Flux and Argo CD
   (`vars/fluxcdDeploy.groovy`, `vars/argocdDeploy.groovy`); neither is wired.
   Flux was chosen because it needs no UI, ingress or SSO, and its controllers
   run on Fargate as-is. Bootstrap it against the manifests repository at path
   `clusters/dev`, which creates the `flux-system` GitRepository that
   `clusters/dev/delivery-demos.yaml` refers to:

   ```sh
   flux bootstrap github --owner=CMS-Enterprise \
     --repository=delivery-pipeline-manifests --branch=main --path=clusters/dev \
     --registry=artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/fluxcd
   ```

   Run the `flux` CLI from a pinned image, never a downloaded binary. Then pin
   the controller images by digest in `clusters/dev/flux-system/kustomization.yaml`
   (a kustomize `images:` entry per controller) and commit that. The flux-system
   deploy key or token is a new credential. Give it a rotate script, as AGENTS.md
   requires.

2. **AWS Load Balancer Controller v3.6.3** in `kube-system`. Commit a Flux
   `HelmRelease` under `clusters/dev/` with these values:
   `clusterName` = output `cluster_name`, `vpcId` = output `vpc_id`,
   `region`, `serviceAccount.name=aws-load-balancer-controller`,
   `serviceAccount.annotations."eks.amazonaws.com/role-arn"` = output
   `lb_controller_role_arn`, and `image.repository`/`image.digest` pointing at the
   IronBank controller image in Artifactory. The chart should come from an
   Artifactory Helm remote. `vpcId` and `region` are required on Fargate because
   there is no instance metadata. The chart creates the `alb` IngressClass the
   Ingresses use. If you upgrade the controller, replace
   `policies/aws-load-balancer-controller.json` with that version's
   `docs/install/iam_policy.json`.

3. **Copy `manifests/` into `delivery-pipeline-manifests`** at the repository
   root (`base/`, `overlays/dev/`, `clusters/dev/`).

4. **Image pull Secret.** Every Deployment pulls with `imagePullSecrets: jfrog-pull`
   in `delivery-demos-dev`. Secrets do not go in git. Create it from the
   `jfrog-credentials` Jenkins credential the same way
   `vars/imagePullSecret.groovy` does, but targeting the EKS cluster and
   namespace instead of the agent's own namespace. Re-sync it after each
   rotation, or rotation breaks new pulls.

5. **ACM certificate** for `*.dev.delivery-demos.example.com` in the cluster's
   account and region. The controller finds it from the Ingress TLS hosts.

## The workloads

Each `base/<service>.yaml` holds one Deployment, PodDisruptionBudget, Service and
Ingress. Ports and UIDs come from each Dockerfile's `EXPOSE`/`USER`, and health
paths come from `appTargets` in `demos/Jenkinsfile`:

| Service | Port | UID | Health | CPU / memory |
| --- | --- | --- | --- | --- |
| nextjs | 3000 | 1000 | `/` | 250m / 512Mi |
| vue | 3001 | 101 | `/` | 250m / 256Mi |
| sveltekit | 3002 | 1000 | `/` | 250m / 512Mi |
| django | 3003 | 1001 | `/health` | 250m / 512Mi |
| maven | 3004 | 1000 | `/health` | 500m / 1Gi |
| go | 3005 | 1000 | `/health` | 250m / 256Mi |
| gradle | 3006 | 1000 | `/health` | 500m / 1Gi |

- Requests equal limits, because Fargate sizes each pod from its requests and
  rounds up to the next vCPU/memory combination.
- A startup probe allows up to 3 minutes for JVM start-up. After that,
  readiness and liveness probes hit the health path on the named `http` port.
- Pods run as non-root with no privilege escalation, all capabilities dropped,
  `RuntimeDefault` seccomp and no service account token. The namespace enforces
  the `restricted` Pod Security Standard. The root filesystem is read-only
  (Trivy KSV-0014); each pod mounts `emptyDir` volumes, capped at 256Mi, only
  where its app writes: `/tmp` for all of them, plus `/app/data` for django's
  start-up `migrate` to SQLite, `.next/cache` for nextjs, and
  `/var/cache/nginx` (pid, temp files) and `/var/log/nginx` for vue. The
  mounts are unverified against running pods; a write elsewhere fails with
  "Read-only file system" and needs another mount.
- Routing is host-based (`<service>.dev.delivery-demos.example.com`) on one
  shared internal ALB (`group.name: delivery-demos`) with IP targets, HTTPS only.
  It is not path-based, because each app serves from `/` and an ALB cannot
  rewrite paths. This differs from the "`${dev_base_url}/<name>`" assumption in
  the commented-out Dev Verification stage.

### Timeout and rollback

- **Timeout.** Each Deployment has `progressDeadlineSeconds: 300`. The Flux
  Kustomization uses `wait: true` with `timeout: 10m`, so it goes `Ready=False`
  when any Deployment fails to roll out. `fluxcdDeploy` (`flux reconcile
  kustomization delivery-demos-dev --timeout ...`) fails the pipeline stage on
  that.
- **Blast radius.** `maxUnavailable: 0` keeps the previous ReplicaSet serving
  while a bad version fails to become ready, so a failed rollout does not take
  the service down.
- **Rollback: revert the gitops commit.** Flux has no automatic rollback for
  plain Kustomizations; only HelmRelease remediation rolls back. The rollback
  is `git revert` of the `gitopsPromote` commit in `delivery-pipeline-manifests`,
  which Flux then reconciles. `kubectl rollout undo` (`vars/rollbackK8s.groovy`)
  does not fit here, because Flux re-applies the git state on its next interval.
  To make the correction happen without manual intervention, the pipeline
  needs a `post { failure }` on the deploy stage that reverts that commit and
  reconciles again. That step is **not wired yet**; it would need a change to
  `demos/Jenkinsfile`.

## How images and tags reach the cluster

1. The pipeline builds each changed service into `itopsplatf-cloudpipe` (staging),
   scans it, promotes it to `itopsplatf-prod`, then cosign-signs it and verifies
   the signature (Image Tests).
2. **Update Image Tags** runs `gitopsPromote` with `verifiedImages`. In
   `delivery-pipeline-manifests/overlays/dev` it runs
   `kustomize edit set image <service>=artifactory.cloud.cms.gov/itopsplatf-prod/<service>:<short sha>`
   for each promoted service and pushes the commit. This only works because
   each base container's `image:` is the service name (with a placeholder
   tag), and kustomize matches on the name whatever the tag.
3. Flux picks up the commit within `interval` (or immediately on
   `fluxcdDeploy`), applies `overlays/dev`, and waits for the rollouts.

`gitopsPromote` only rewrites the services that changed, so the `newTag:
set-by-pipeline` placeholders stay until the first run that builds all seven
services. Run that first run (or set the tags by hand) before Flux reconciles,
or those Deployments sit in `ImagePullBackOff`. Their failure stays contained
to them.

The cluster pulls by tag, so the signature check happens in the pipeline, not
at admission. An admission policy that verifies cosign signatures against
`demos/cosign.pub` (sigstore policy-controller or Kyverno `verifyImages`),
together with digest pins written by `gitopsPromote`, would close that gap. Both
are follow-ups.

## Teardown

Tear down in reverse order, so the controller can delete the ALB and its
security groups before the cluster goes away:

1. Remove the Ingresses, or suspend and delete the `delivery-demos-dev`
   Kustomization with `prune: true`. Wait until the ALB is gone from the EC2
   console or `aws elbv2 describe-load-balancers`.
2. Uninstall the load balancer controller and Flux (`flux uninstall`).
3. Run `awsAssumeRole`, then `terraform destroy -var-file=dev.tfvars` from the
   same IronBank image. The KMS key enters a 7-day deletion window. If
   `tag_subnets_for_internal_alb` was true, destroy removes those subnet tags.

If you skip step 1, the ALB, target groups and their security groups are
orphaned. They then block subnet and VPC changes until you delete them by hand.

## Not yet verified

- `terraform fmt -check` passes under the workstation's terraform 1.14.8.
  `terraform validate`, tflint and trivy have not run: validate needs
  `terraform init`, and the IronBank 1.15 image is amd64-only.
- The lock file was copied, not generated. It holds for as long as only
  `hashicorp/aws ~> 6.0` is required. `terraform init -lockfile=readonly`
  confirms it.
- `kubectl kustomize overlays/dev` builds (29 objects). The manifests have not
  been applied to a cluster.
- `kubernetes_version = "1.35"` and the CoreDNS `configuration_values` schema
  should be checked with `aws eks describe-addon-configuration` for that version.
