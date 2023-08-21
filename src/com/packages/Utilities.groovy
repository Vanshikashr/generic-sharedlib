package com.packages
// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}

// notify user
def notifyUserStarted(MODULE, ENV, BUILD_NUMBER, REGION_NAME, REPOSITORY_NUMBER, DEFAULT_ENV, DEFAULT_PROJECT_PREFIX, PROJECT_NAME) {
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
