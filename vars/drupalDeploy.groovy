#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
//import com.packages.Deploy

def call( ENV , BRANCH_NAME, MODULE,DEFAULT_BRANCH,  REGION_NAME, REPOSITORY_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX, PROJECT_NAME, S3_BUCKET_NAME, S3_BUCKET_PATH, DOCKER_IMAGE_NAME)
{
    build = new Build()
    utilities = new Utilities()
    //deploy = new Deploy()
pipeline {
    agent any
    parameters {
         choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'MODULE', choices: ['drupal-app'], description: 'Choose module to build')
        string(name: 'BRANCH_NAME', defaultValue: 'ttn-infra', description: 'Git Branch Name')
        
       
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
                   
                     utilities.setupEnvironments(BUILD_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX,MODULE,REPOSITORY_NUMBER,REGION_NAME,ENV,BRANCH_NAME)
                  
                }
            }
        }
        
        stage("Download docker config") {
            steps {
                script {
                 utilities.downloadDockerConfigFromS3(S3_BUCKET_NAME,S3_BUCKET_PATH,REGION_NAME)
            }
            }
        }
        
        stage("Docker Image Push") {
            steps {

                script {
                utilities.dockerImagePush(REGION_NAME,REPOSITORY_NUMBER,DOCKER_IMAGE_NAME)
                   
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
