#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
import com.packages.Deploy

def call( ENV ,  DEFAULT_BRANCH, BRANCH_NAME, MODULE,  REGION_NAME, REPOSITORY_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX, PROJECT_NAME)
{
    build = new Build()
    utilities = new Utilities()
    deploy = new Deploy()
pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'MODULE', choices: ['react'], description: 'Choose module to build')
        string(name: 'BRANCH_NAME', description: 'Branch')
        
       
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
    
        stage("Cleaning workspace") {
            steps {
                script {
                    
                   utilities.cleanWorkspace()
                }
            }
        }
       stage("Notify User started") {
            steps {
                script {
                   utilities.notifyUserStarted (MODULE, ENV,  REGION_NAME, REPOSITORY_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX, PROJECT_NAME)
                }
            }
       }
        stage("Pulling the Repository") {
            steps {
                script {
                utilities.pullRepository(BRANCH_NAME,ENV,DEFAULT_BRANCH,MODULE)
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
