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
       stage('Building and Pushing Docker Image') {
                steps {            
                    script {
                        utilities.dockerLoginEcr()    
                        build.dockerBuild()
                        utilities.dockerPush()
                    }
                }
            }
        
         stage('ECS deploy') {
               steps {
                  script {
                      deploy.ecsDeploy()
                    }
                }
            }
     
      stage('Docker Image Cleanup'){
                steps{
                   script{
                       utilities.cleanupDockerImage()
                    }
                }
            }
        
      
  }
}
}

