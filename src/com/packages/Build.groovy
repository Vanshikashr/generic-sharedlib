def setBuildInfo(env, branch, module) {
    wrap([$class: 'BuildUser']) {
        script {
            def changeLogSets = currentBuild.changeSets
            currentBuild.displayName = "#${env}-#${branch}-#${currentBuild.number}"
            currentBuild.description = "Module: ${module} \n Build By: ${BUILD_USER}"
        }
    }
}


