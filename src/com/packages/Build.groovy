package com.packages
def dockerBuild(containerRegistoryUrl, appName,S3_BUCKET_NAME, S3_BUCKET_PATH, REGION_NAME) {
    def containerRegistoryUrl = env.containerRegistoryUrl
    def appName =  params.appName

    branchName = "$GIT_BRANCH" 
    def dockerImageName = "${containerRegistoryUrl}/${appName}:${dockerImageTag}"
    def dockerEnvTag = "${containerRegistoryUrl}/${appName}:${branchName}-latest"
    // Build Docker image
    sh "aws s3 cp s3://${S3_BUCKET_NAME}/${S3_BUCKET_PATH} . --recursive --region  ${REGION_NAME}
    sh "docker build -t ${dockerImageName} -t ${dockerEnvTag} -f Dockerfile ."
 
    
}
