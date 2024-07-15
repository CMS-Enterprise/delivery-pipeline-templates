## Mulitbranch Pipeline

To create a Jenkins Multibranch Pipeline Project with branch sources as Git and credentials using a deploy key, follow these steps:

### Prerequisites
1. **Jenkins Access:** Ensure access to [Cloudbee Jenkins URL](https://jenkins-east.cloud.cms.gov/).
2. **Git Repository:** Have a Git repository with a `Jenkinsfile` at the specified path.
3. **Deploy Key:** Ensure you have a deploy key added to your Git repository for read access.


### Step-by-Step Guide

#### 1. Create a New Multibranch Pipeline Project

1. **Login to Jenkins:**
   - Open your Jenkins URL in a web browser and log in with your credentials.

2. **Create a New Item:**
   - Click on `New Item` on the Jenkins dashboard.
   - Enter a folder name of the project.
   ![](<static/images/Multibranch - App-Name Folder.png>)
   - Select the folder name of the project to configure
   ![](<static/images/Multibranch - Folder Configuration.png>)

3. **Create a New Item:**
   - Click on `New Item` on the Jenkins dashboard.
   - Enter a name for your project.
   - Select `Multibranch Pipeline` and click `OK`.
   ![](<static/images/Multibranch - Multibranch Pipeline.png>)

#### 2. Configure Branch Sources

1. **Branch Sources Configuration:**
   - In the project configuration page, scroll down to the `Branch Sources` section.
   - Click `Add Source` and select `Git`.

2. **Configure Git Source:**
   - **Project Repository:** Enter the repository URL (e.g., `git@github.com:username/repo.git`).
   ![](<static/images/Multibranch - MB Git Configuration.png>)
   - **Credentials:** Click `Add` to add a new SSH credential.

#### 3. Add SSH Credential (Deploy Key)

1. **Add SSH Credentials:**
   - In the `Kind` dropdown, select `SSH Username with private key`.
   - **ID:** Enter an identifier for the credential (e.g., `deploy-key`).
   - **Username:** Enter a username (usually `git` for GitHub).
   - **Private Key:** Select `Enter directly` and paste your deploy key here.
   - Click `Add` to save the credential.

2. **Select Credential:**
   - Back in the `Branch Sources` configuration, select the added credential from the `Credentials` dropdown.

#### 4. Configure Build Configuration

1. **Build Configuration:**
   - Scroll down to the `Build Configuration` section.
   - Select `by Jenkinsfile`.

2. **Script Path:**
   - Enter the path to your `Jenkinsfile` in the repository (e.g., `Jenkinsfile`).
   ![](<static/images/Multibranch - MB Jenkinsfile Configuration.png>)   

#### 5. Additional Configuration

1. **Property Strategy:**
   - Configure any additional properties or behaviors as required for your project (e.g., branch discovery, filtering by name, etc.).

2. **Save Configuration:**
   - Click `Save` at the bottom of the configuration page.



### References
- [Jenkins Multibranch Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/multibranch/)
- [Adding SSH Credentials in Jenkins](https://www.jenkins.io/doc/book/using/using-credentials/#ssh-user-private-key)
- [GitHub Deploy Keys Documentation](https://docs.github.com/en/developers/overview/managing-deploy-keys)

By following these steps, you can set up a Jenkins Multibranch Pipeline Project that uses a Git repository with deploy key credentials and specifies the Jenkinsfile path.

## Delivery Pipeline

Creating a Jenkins Delivery Pipeline Project using a delivery pipeline template involves setting up a job in Jenkins that will handle the delivery of your application. The configuration includes specifying an image, an image tag, a JSON file for Docker authentication, and a Dockerfile. Below are the step-by-step instructions along with screenshots to guide you through the process.


### Step-by-Step Guide

#### 1. Create a New Pipeline Project


1. **Create a New Item:**
   - Click on `New Item` on the Jenkins dashboard.
   - Enter a name for your project (e.g., `App-Name-Delivery`).
   - Select `Delivery Pipeline` and click `OK`.
   ![](<static/images/Multibranch - Delivery Pipeline.png>)

#### 2. Configure Pipeline

1. **Pipeline Configuration:**
   - In the project configuration page, Update the Pipeline for building, scanning, and publishing a container image.

2. **Add Parameters:**
   - Add the following parameters:
     - **String Parameter:**
       - **Name:** `image`
       - **Default Value:** `your-docker-image` (e.g., `docker.io/my-app`)
     - **String Parameter:**
       - **Name:** `Image tag`
       - **Default Value:** `latest`

#### 3. Add Credentials

1. **Add Docker Authentication:**
   - Go to `Manage Jenkins` -> `Manage Credentials`.
   - Select a domain (e.g., `global`).
   - Click `Add Credentials`.
   - Select `Secret file` from the `Kind` dropdown.
   - Upload your Docker config JSON file and give it an ID (e.g., `docker-config-file`).

2. **Select Credential:**
   - Back in the configuration, select the added credential from the `Credentials` dropdown.
     - **File Parameter:**
       - **Name:** `JSON File Docker Config for Authentication (Secret File)`
       - **Default Value:** `Choose the approriate key from credentials dropdown list`   

#### 4. Save the Pipeline

1. **Save Configuration:**
   - Click `Save` at the bottom of the configuration page.
![](<static/images/Multibranch - Delivery Pipeline configurartion.png>)

### References
- [Managing Credentials in Jenkins](https://www.jenkins.io/doc/book/using/using-credentials/)
- [jenkins-multibranch-pipeline-with-git-tutorial](https://www.cloudbees.com/blog/jenkins-multibranch-pipeline-with-git-tutorial)

By following these steps, you can create a Jenkins Delivery Pipeline Project that uses a Docker image, image tag, Docker authentication via JSON file, and a Dockerfile. This setup will automate the process of building and pushing your Docker images.

## SAST Pipeline

Here's a step-by-step guide to creating a Jenkins SAST (Static Application Security Testing) pipeline project using a SAST pipeline template with the provided parameters. This pipeline will integrate SonarQube for scanning source code for potential security defects.

### Prerequisites

1. **SonarQube Access:** Ensure Accessto [SonarQube URL](https://sonarqube.cloud.cms.gov/).
2. **SonarQube Token:** Generate a token from SonarQube for API access.
### Step-by-Step Guide

#### 1. Create a New Pipeline Project

1. **Create a New Item:**
   - Click on `New Item` on the Jenkins dashboard.
   - Enter a name for your project (e.g., `App-Name-SAST`).
   - Select `SAST Pipeline` and click `OK`.
![](<static/images/Multibranch - SAST Pipeline.png>)

#### 2. Configure Pipeline

1. **Pipeline Configuration:**
   - In the project configuration page, Update Pipeline for scanning a source code for potential security and other defects.

2. **Add Parameters:**
   - Add the following parameters:
     - **String Parameter:**
       - **Name:** `SonarQube Url`
       - **Default Value:** `https://sonarqube.cloud.cms.gov`
     - **String Parameter:**
       - **Name:** `SonarQube Project Key`
       - **Default Value:** `App-Name-Key`
     - **String Parameter:**
       - **Name:** `SonarQube Additional Arguments`
       - **Default Value:** `[]`
     - **String Parameter:**
       - **Name:** `Source Path`
       - **Default Value:** `.`

#### 3. Add Credentials

1. **Add SonarQube Token:**
   - Go to `Manage Jenkins` -> `Manage Credentials`.
   - Select a domain (e.g., `global`).
   - Click `Add Credentials`.
   - Select `Secret text` from the `Kind` dropdown.
   - Provide your SonarQube token and give it an ID (e.g., `sonarqube-token`).
   
2. **Select Credential:**
   - Back in the configuration, select the added credential from the `Credentials` dropdown.
       - **String Parameter:**
       - **Name:** `SonarQube Token`
       - **Default Value:** `` 

#### 4. Save the Pipeline

1. **Save Configuration:**
   - Click `Save` at the bottom of the configuration page.
![](<static/images/Multibranch - SAST Pipeline Configuration.png>)

By following these steps, you can create a Jenkins SAST pipeline project that scans source code for potential security and other defects using SonarQube.

## Deployment Pipeline

Here's a step-by-step guide to creating a Jenkins deployment pipeline project using a deployment pipeline template to update Kubernetes manifests using Kustomize. This example includes configurations for the manifest repository URL, Git credentials, Git branch, author details, image, default environment path, and default target service.

### Step-by-Step Guide

#### 1. Create a New Pipeline Project

1. **Create a New Item:**
   - Click on `New Item` on the Jenkins dashboard.
   - Enter a name for your project (e.g., `App-Name-Deployment`).
   - Select `GitOps Deployment` and click `OK`.
![](<static/images/Multibranch - Deployment Pipeline.png>)
#### 2. Configure Pipeline

1. **Pipeline Configuration:**
   - In the project configuration page, Update the Pipeline for updating Kubernetes manifests using Kustomize section.

2. **Add Parameters:**
   - Add the following parameters:
     - **String Parameter:**
       - **Name:** `Deployment manifest repo url`
       - **Default Value:** `git_manifest_repository`
     - **String Parameter:**
       - **Name:** `Git Branch`
       - **Default Value:** `main`
     - **String Parameter:**
       - **Name:** `Git Author Name`
       - **Default Value:** `Deployment Pipeline`
     - **String Parameter:**
       - **Name:** `Git Authour Email`
       - **Default Value:** `your_user@example.com`
     - **String Parameter:**
       - **Name:** `Image`
       - **Default Value:** `The fully qualified container image name.`
     - **String Parameter:**
       - **Name:** `Default Environment Path`
       - **Default Value:** `dev`
     - **String Parameter:**
       - **Name:** `Default Target Service`
       - **Default Value:** `App-Name`

#### 3. Add Credentials

1. **Add Git SSH Credentials:**
   - Go to `Manage Jenkins` -> `Manage Credentials`.
   - Select a domain (e.g., `global`).
   - Click `Add Credentials`.
   - Select `SSH Username with private key` from the `Kind` dropdown.
   - Provide your GitHub Deploy Key and give it an ID (e.g., `git-ssh-credentials`).
2. **Select Credential:**
   - Back in the configuration, select the added credential from the `Credentials` dropdown.
     - **File Parameter:**
       - **Name:** `Git Credentials`
       - **Default Value:** `Choose the approriate key from credentials dropdown list`  

#### 4. Save and Run the Pipeline

1. **Save Configuration:**
   - Click `Save` at the bottom of the configuration page.
![](<static/images/Multibranch - Deployment Pipeline Configuration.png>)

### References

- [Kustomize Documentation](https://kustomize.io/)

By following these steps, you can create a Jenkins deployment pipeline project that updates Kubernetes manifests using Kustomize, configured with parameters for the manifest repository, credentials, branch, author details, image, environment path, and target service.


