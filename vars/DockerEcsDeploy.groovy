#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
import com.packages.Deploy

def call( containerRegistoryUrl, appName,S3_BUCKET_NAME, S3_BUCKET_PATH, awsRegion)
{
    build = new Build()
    utilities = new Utilities()
    deploy = new Deploy()
pipeline {
    agent any
    parameters {
        choice(name: 'ENV', choices: ['dev'], description: 'Choose Environment Name')
        choice(name: 'appName', choices: ['react'], description: 'Choose module to build')
        string(name: 'branchName', description: 'Branch')
        
       
    }
  stages {
       stage('Building and Pushing Docker Image') {
                steps {            
                    script {
                        utilities.dockerLoginEcr( containerRegistoryUrl, awsRegion)    
                        build.dockerBuild(containerRegistoryUrl, appName, S3_BUCKET_NAME, S3_BUCKET_PATH, awsRegion)
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

