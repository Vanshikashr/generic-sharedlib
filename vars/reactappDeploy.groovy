

def call( String DEFAULT_ENV , String DEFAULT_BRANCH, String DEFAULT_PROJECT_PREFIX, String REGION_NAME, String REPOSITORY_NUMBER, String GIT_URL, String S3_BUCKET_NAME, String S3_BUCKET_PATH, String HELM_REPO, String HELM_BRANCH, String KUBE_CONFIG) {


pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'MODULE', choices: ['react'], description: 'Choose module to build')
        string(name: 'BRANCH', description: 'Branch')
        
       
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
                pullRepository(params.BRANCH, env.GIT_URL)
            }
        }
        
        stage("Building the Artifacts") {
            steps {
                buildArtifacts(env.S3_BUCKET_NAME, env.S3_BUCKET_PATH, env.REGION_NAME)
            }
        }
        
        stage("Docker Image Push") {
            steps {
                dockerImagePush(
                    
                    env.REGION_NAME,
                    env.REPOSITORY_NUMBER,
                    
            
                    
                    
               
                )
            }
        }
        
        stage("Deploying App") {
            steps {
                deployApp(
                    env.HELM_BRANCH,
                    env.HELM_REPO,
                    params.ENV,
                    params.MODULE,
                 
                    env.KUBE_CONFIG
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
