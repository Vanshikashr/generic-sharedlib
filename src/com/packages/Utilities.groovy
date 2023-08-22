package com.packages
// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}

// notify user
def notifyUserStarted(MODULE, ENV,  REGION_NAME, REPOSITORY_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX, PROJECT_NAME) {
    def deployment_details = ""
    def postfix = ", "
    if (MODULE == "drupal-app") {
        deployment_details = deployment_details + "Drupal App" + postfix
    }

    sh """#!/bin/bash
        echo "Hi, Jenkins deployment started for ${deployment_details} having build number ${BUILD_NUMBER}" > message.txt
        aws sns publish --topic-arn "arn:aws:sns:${REGION_NAME}:${REPOSITORY_NUMBER}:${DEFAULT_ENV}-${DEFAULT_PROJECT_PREFIX}-info-alert" \
        --message file://message.txt --subject "${PROJECT_NAME} | ${ENV} | SUCCESS - Jenkins Deployment Started" \
        --region ${REGION_NAME}
    """
}

// pull repository
def pullRepository(BRANCH_NAME,ENV,DEFAULT_BRANCH,MODULE) {
    echo "Branch: ${BRANCH_NAME}"
    echo "Environment: ${ENV}"
    script {
        //def BRANCH_NAME = BRANCH?: "${DEFAULT_BRANCH}"
        def GIT_URL = ""
        if (MODULE == "drupal-app") {
            GIT_URL = "git@gitlab.intelligrape.net:tothenew/ttnd-website-drupal.git"
        }
        git branch: "${BRANCH_NAME}", changelog: true, poll: true, url: "${GIT_URL}"
    }
}

// setting up environment variables
def setupEnvironments(BUILD_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX,MODULE,REPOSITORY_NUMBER,REGION_NAME,ENV,BRANCH_NAME) {
    def COMMIT_ID = sh(returnStdout: true, script: "git rev-parse --short HEAD").trim()
    def ARTIFACT_VERSION = "${BUILD_NUMBER}-${COMMIT_ID}"

    def REPOSITORY_NAME = "${DEFAULT_ENV}-${DEFAULT_PROJECT_PREFIX}-${MODULE}"
    def IMAGE_NAME = "${REPOSITORY_NUMBER}.dkr.ecr.${REGION_NAME}.amazonaws.com/${REPOSITORY_NAME}"

    def DOCKER_IMAGE_TAG = "${ENV}_${BRANCH_NAME}_${COMMIT_ID}"
    def ECS_IMAGE_TAG = "${ENV}_latest"

    def DOCKER_IMAGE_NAME = "${IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
    def ECS_IMAGE_NAME = "${IMAGE_NAME}:${ECS_IMAGE_TAG}"

    def CLUSTER_NAME = "${ENV}-${DEFAULT_PROJECT_PREFIX}"
    def ECS_PREFIX = "${ENV}-${DEFAULT_PROJECT_PREFIX}-${MODULE}"
    def TASK_NAME = "${ECS_PREFIX}"
    def SERVICE_NAME = "${ECS_PREFIX}"

    println "ARTIFACT_VERSION: $ARTIFACT_VERSION" 
    println "REPOSITORY_NAME: $REPOSITORY_NAME" 
    println "IMAGE_NAME: $IMAGE_NAME" 
    println "DOCKER_IMAGE_NAME: $DOCKER_IMAGE_NAME" 
    println "CLUSTER_NAME: $CLUSTER_NAME" 
    println "ECS_IMAGE_NAME: $ECS_IMAGE_NAME" 
    println "TASK_NAME: $TASK_NAME"
    println "SERVICE_NAME: $SERVICE_NAME" 


    
   
}
// download from s3
def downloadDockerConfigFromS3(S3_BUCKET_NAME,S3_BUCKET_PATH,REGION_NAME) {
    sh """#!/bin/bash
        set -xe
        echo "env=${ENV}" > $WORKSPACE/version
     
        
        if [ "${MODULE}" == "drupal-app" ]
        then
            aws s3 cp s3://${S3_BUCKET_NAME}/${S3_BUCKET_PATH}/${MODULE}/ ./ --recursive --region ${REGION_NAME}
        fi
    """
}

// docker push
def dockerImagePush(REGION_NAME, REPOSITORY_NUMBER, DOCKER_IMAGE_NAME) {
    sh """#!/bin/bash
        set -xe
        echo $WORKSPACE

        echo "Docker Image Push"
        aws ecr get-login-password --region ${REGION_NAME} | docker login --username AWS --password-stdin ${REPOSITORY_NUMBER}.dkr.ecr.${REGION_NAME}.amazonaws.com
        docker rmi -f ${DOCKER_IMAGE_NAME}
        docker buildx build --platform linux/arm64 --provenance=false -f Dockerfile --build-arg artifact_version=${COMMIT_ID} -t ${DOCKER_IMAGE_NAME} -t ${ECS_IMAGE_NAME} --push .

        if [ \$? -eq 0 ]
        then
            echo "Successfully image tagged and pushed to repository"
            echo ${DOCKER_IMAGE_NAME} > $WORKSPACE/image_id
            cat $WORKSPACE/image_id
        else
            echo "Error in tagging/pushing image"
            exit 1
        fi
    """
}
