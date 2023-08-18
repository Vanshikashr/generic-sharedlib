// build done by whom and id and all
def setBuildInfo() {
    wrap([$class: 'BuildUser']) {
      
            def changeLogSets = currentBuild.changeSets
            currentBuild.displayName = "#${ENV}-#${BRANCH}"
            currentBuild.description = "Module: ${MODULE}"
        
    }
}

// build artifact
def buildArtifacts() {
    
        sh """#!/bin/bash
            set -xe
            echo $WORKSPACE
            aws s3 cp s3://${S3_BUCKET_NAME}/${S3_BUCKET_PATH} . --recursive --region ${REGION_NAME}
        """
    
}


