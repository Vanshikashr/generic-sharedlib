#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
//import com.packages.Deploy

def call( ENV ,  BRANCH_NAME, MODULE,  DEFAULT_PROJECT_PREFIX,  REGION_NAME, REPOSITORY_NUMBER,  GIT_URL,  S3_BUCKET_NAME, S3_BUCKET_PATH,  HELM_REPO, HELM_BRANCH,  KUBE_CONFIG)
{
    build = new Build()
    utilities = new Utilities()
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
                    sh '''
                    echo "hello from Build Info Step"
                    '''
                  build.setBuildInfo()
                }
            }
        }
        
        stage("Pulling the Repository") {
            steps {
                script {
                utilities.pullRepository(GIT_URL)
            }
            }
        }
      stage("Setting up the Environments") {
            steps {
                script {
                    println("==========================HERE========================")
                    def envData = utilities.setupEnvironments()
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
        
        stage("Building the Artifacts") {
            steps {
                script {
                build.buildArtifacts(env.S3_BUCKET_NAME, env.S3_BUCKET_PATH, env.REGION_NAME)
            }
            }
        }
        
        stage("Docker Image Push") {
            steps {

                script {
                utilities.dockerImagePush(
                    env.REGION_NAME,
                    env.REPOSITORY_NUMBER
                )
                }
            }
        }
        
      
     
        
  } 
    }
}
