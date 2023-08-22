package com.packages
// build done by whom and id and all
def setBuildInfo() {
    wrap([$class: 'BuildUser']) {
      
            def changeLogSets = currentBuild.changeSets
            currentBuild.displayName = "#${ENV}-#${BRANCH_NAME}"
            currentBuild.description = "Module: ${MODULE}"
        
    }
}
