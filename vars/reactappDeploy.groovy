#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
import com.packages.Deploy

def call( ENV ,  BRANCH_NAME, MODULE,  DEFAULT_PROJECT_PREFIX,  REGION_NAME, REPOSITORY_NUMBER,  GIT_URL,  S3_BUCKET_NAME, S3_BUCKET_PATH,  HELM_REPO, HELM_BRANCH,  KUBE_CONFIG)
{
    build = new Build()
    utilities = new Utilities()
    deploy = new Deploy()
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
                utilities.pullRepository(GIT_URL,BRANCH_NAME)
            }
            }
        }
      stage("Setting up the Environments") {
            steps {
                script {
                   
                     utilities.setupEnvironments(BUILD_NUMBER,DEFAULT_PROJECT_PREFIX,MODULE,REPOSITORY_NUMBER,REGION_NAME,ENV,BRANCH_NAME)
                  
                }
            }
        }
        
        stage("Building the Artifacts") {
            steps {
                script {
                build.buildArtifacts(S3_BUCKET_NAME,S3_BUCKET_PATH,REGION_NAME)
            }
            }
        }
        
        stage("Docker Image Push") {
            steps {

                script {
                utilities.dockerImagePush(REGION_NAME,REPOSITORY_NUMBER)
                   
                }
            }
        }
        
         stage("Deploy") {
            steps {

                script {
                deploy.deployApp(HELM_BRANCH,HELM_REPO,ENV,MODULE,KUBE_CONFIG)
     
        
  } 
    }
}
      
  }
}
}
