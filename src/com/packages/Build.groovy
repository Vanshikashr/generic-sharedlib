// build done by whom and id and all
def setBuildInfo(String env, String branch, String module) {
    wrap([$class: 'BuildUser']) {
      
            def changeLogSets = currentBuild.changeSets
            currentBuild.displayName = "#${env}-#${branch}-#${currentBuild.number}"
            currentBuild.description = "Module: ${module} \n Build By: ${BUILD_USER}"
        
    }
}

// build artifact
def buildArtifacts(String workspace, String s3BucketName, String s3BucketPath, String regionName) {
    
        sh """#!/bin/bash
            set -xe
            echo $workspace
            aws s3 cp s3://${s3BucketName}/${s3BucketPath} . --recursive --region ${regionName}
        """
    
}


