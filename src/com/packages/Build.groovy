package com.packages
def dockerBuild() {
    def containerRegistoryUrl = env.containerRegistoryUrl
    def appName =  env.appName
    def dockerFilePath = env.dockerFilePath != 'null' ? env.dockerFilePath : 'docker/Dockerfile'
    println("Docker file path: " + dockerFilePath)
    if (fileExists(dockerFilePath)){
    // Define Docker image name and tag
    branchName = "$GIT_BRANCH" 
    def dockerImageName = "${containerRegistoryUrl}/${appName}:${dockerImageTag}"
    def dockerEnvTag = "${containerRegistoryUrl}/${appName}:${branchName}-latest"
    // Build Docker image
    sh "docker build -t ${dockerImageName} -t ${dockerEnvTag} -f ${dockerFilePath} ."
    // sh "docker tag ${dockerImageName} ${dockerImageName2}"
    
    }
    else{
      error 'Dockerfile does not exist on given path.'
    }
}
