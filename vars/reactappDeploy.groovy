

def call( DEFAULT_ENV , DEFAULT_BRANCH, DEFAULT_PROJECT_PREFIX, REGION_NAME, REPOSITORY_NUMBER, GIT_URL, S3_BUCKET_NAME, S3_BUCKET_PATH, HELM_REPO, HELM_BRANCH, KUBE_CONFIG) {


pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'MODULE', choices: ['react'], description: 'Choose module to build')
        string(name: 'BRANCH', description: 'Branch')
        string(name: 'GIT_URL', description: ' Giturl')
        string(name: 'PROJECT_PREFIX', description: 'prefix')
        string(name: 'S3_BUCKET_NAME', description: 'S3 bucket name')
        string(name: 'S3_BUCKET_PATH', description: 'S3 bucket path')
        string(name: 'REGION_NAME', description: 'Region name')
        
       
        string(name: 'REPOSITORY_NUMBER', description: 'Repository number')
        string(name: 'HELM_BRANCH', description: 'Helm branch')
        string(name: 'HELM_REPO', description: 'Helm repository URL')
    
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
                pullRepository(params.BRANCH, params.GIT_URL)
            }
        }
        
        stage("Building the Artifacts") {
            steps {
                buildArtifacts(params.S3_BUCKET_NAME, params.S3_BUCKET_PATH, params.REGION_NAME)
            }
        }
        
        stage("Docker Image Push") {
            steps {
                dockerImagePush(
                    
                    params.REGION_NAME,
                    params.REPOSITORY_NUMBER,
                    
            
                    
                    
               
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
