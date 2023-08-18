
def call(envValue, branchValue, moduleValue, workspaceValue, s3BucketNameValue, s3BucketPathValue, regionNameValue, dockerImageNameValue, eksImageNameValue, commitIdValue, dockerImageTagValue, repositoryNameValue, helmBranchValue, helmRepoValue, kubeConfigValue) {


pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'MODULE', choices: ['react'], description: 'Choose module to build')
        choice(name: 'BRANCH', choices: ['NUD-27','main','alpha-test'], description: 'Git Branch Name')
       string(name: 'WORKSPACE', description: 'Workspace path')
        string(name: 'S3_BUCKET_NAME', description: 'S3 bucket name')
        string(name: 'S3_BUCKET_PATH', description: 'S3 bucket path')
        string(name: 'REGION_NAME', description: 'Region name')
        string(name: 'DOCKER_IMAGE_NAME', description: 'Docker image name')
        string(name: 'EKS_IMAGE_NAME', description: 'EKS image name')
        string(name: 'COMMIT_ID', description: 'Commit ID')
        string(name: 'DOCKER_IMAGE_TAG', description: 'Docker image tag')
        string(name: 'REPOSITORY_NAME', description: 'Repository name')
        string(name: 'HELM_BRANCH', description: 'Helm branch')
        string(name: 'HELM_REPO', description: 'Helm repository URL')
        string(name: 'DOCKER_IMAGE_TAG', description: 'Docker image tag')
        string(name: 'KUBE_CONFIG', description: 'Kube config')
       
    }
  stages {
        stage("Setting Build") {
            steps {
                script {
                   setBuildInfo(params.ENV, params.BRANCH, params.MODULE)
                }
            }
        }
        
        stage("Pulling the Repository") {
            steps {
                pullRepository(params.BRANCH, params.ENV)
            }
        }
        
        stage("Building the Artifacts") {
            steps {
                buildArtifacts(params.WORKSPACE, params.S3_BUCKET_NAME, params.S3_BUCKET_PATH, params.REGION_NAME)
            }
        }
        
        stage("Docker Image Push") {
            steps {
                dockerImagePush(
                    params.WORKSPACE,
                    params.REGION_NAME,
                    params.REPOSITORY_NUMBER,
                    params.DOCKER_IMAGE_NAME,
                    params.EKS_IMAGE_NAME,
                    params.COMMIT_ID,
                    params.DOCKER_IMAGE_TAG,
                    params.REPOSITORY_NAME
                )
            }
        }
        
        stage("Deploying App") {
            steps {
                deployApp(
                    params.HELM_BRANCH,
                    params.HELM_REPO,
                    params.ENV,
                    params.MODULE,
                    params.DOCKER_IMAGE_TAG,
                    params.KUBE_CONFIG
                )
            }
        }
       stage("Setting up the Environments") {
            steps {
                script {
                    def envData = utilities.setupEnvironments(params.ENV, params.BRANCH, params.MODULE)
                    println("========================================================================")
                    println("ARTIFACT_VERSION: " + envData.ARTIFACT_VERSION)
                    println("REPOSITORY_NAME: " + envData.REPOSITORY_NAME)
                    println("IMAGE_NAME: " + envData.IMAGE_NAME)
                    println("DOCKER_IMAGE_NAME: " + envData.DOCKER_IMAGE_NAME)
                    println("EKS_IMAGE_NAME: " + envData.EKS_IMAGE_NAME)
                    println("CLUSTER_NAME: " + envData.CLUSTER_NAME)
                    println("TASK_NAME: " + envData.TASK_NAME)
                    println("SERVICE_NAME: " + envData.SERVICE_NAME)
                }
            }
        }
        
  } 
    }
}
