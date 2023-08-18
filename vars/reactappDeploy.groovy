#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
import com.packages.Deploy

def call( ENV ,  BRANCH_NAME, MODULE,  DEFAULT_PROJECT_PREFIX,  REGION_NAME, REPOSITORY_NUMBER,  GIT_URL,  S3_BUCKET_NAME, S3_BUCKET_PATH,  HELM_REPO, HELM_BRANCH,  KUBE_CONFIG)
{
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
                  build.setBuildInfo(params.ENV, params.BRANCH_NAME, params.MODULE)
                }
            }
        }
        
        stage("Pulling the Repository") {
            steps {
                pullRepository(params.BRANCH_NAME, env.GIT_URL)
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
        
      
       stage("Setting up the Environments") {
            steps {
                script {
                    def envData = setupEnvironments(params.ENV, params.BRANCH_NAME, params.MODULE)
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
