package com.packages

// helm deploy
def helmInstall(){
    echo "Installing ${release} in ${namespace}"
   
        sh """#!/bin/bash
            set -xe
            cd $WORKSPACE/services
            ls
            aws eks --region ap-south-1 update-kubeconfig --kubeconfig=${KUBE_CONFIG} --name non-prod-eupi-eks
            /usr/local/bin/helm upgrade --install --namespace ${namespace} --kubeconfig=${KUBE_CONFIG} ${release} --set image.tag=${DOCKER_IMAGE_TAG} -f ./${MODULE}/values-${namespace}.yaml ./${MODULE}
            /root/bin/kubectl rollout status deployment ${MODULE} --namespace ${namespace} --kubeconfig=${KUBE_CONFIG}
            sleep 120s
        """
    
}


// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}

// clone repository
def pullRepository(GIT_URL,BRANCH_NAME) {
    echo "Branch: ${BRANCH_NAME}"
    //echo "Environment: ${ENV}"
    
    
       // def BRANCH_NAME = branch ?: env.DEFAULT_BRANCH
        
        git branch: "${BRANCH_NAME}", url: "${GIT_URL}", changelog: true, poll: true
    
}   

// docker push
def dockerImagePush() {

        sh """#!/bin/bash
            set -xe
            echo $WORKSPACE

            echo "Docker Image Push"
            aws ecr get-login-password --region ${REGION_NAME} | docker login --username AWS --password-stdin ${REPOSITORY_NUMBER}.dkr.ecr.${REGION_NAME}.amazonaws.com
            docker rmi -f ${DOCKER_IMAGE_NAME}
            docker buildx build --platform linux/amd64,linux/arm64 --provenance=false -f Dockerfile --build-arg artifact_version=${COMMIT_ID} -t ${DOCKER_IMAGE_NAME} -t ${ EKS_IMAGE_NAME } --push .

            if [ \$? -eq 0 ]
            then
                echo "Successfully image tagged and pushed to repository"
                echo ${DOCKER_IMAGE_NAME} > $WORKSPACE/image_id
                cat $WORKSPACE/image_id
            else
                echo "Error in tagging/pushing image"
                exit 1
            fi

            MANIFEST_LIST=`docker manifest inspect ${DOCKER_IMAGE_NAME}`

            # Parse the manifest list to retrieve the SHA IDs for each architecture
            SHA_IDS=()
            for row in `echo "\${MANIFEST_LIST}" | jq -r '.manifests[] | @base64'`; do
                architecture=`echo \${row} | base64 --decode | jq -r '.platform.architecture'`
                digest=`echo \${row} | base64 --decode | jq -r '.digest'`

                if [ "\${architecture}" == "amd64" ]; then
                    echo "Architecture: \${architecture}, SHA ID: \${digest}"
                    amd64_sha=\${digest}
                elif [ "\${architecture}" == "arm64" ]; then
                    echo "Architecture: \${architecture}, SHA ID: \${digest}"
                    arm64_sha=\${digest}
                fi
            done

            AMD_TAG="${DOCKER_IMAGE_TAG}-amd"
            ARM_TAG="${DOCKER_IMAGE_TAG}-arm"

            MANIFEST_AMD=\$(aws ecr batch-get-image --repository-name ${REPOSITORY_NAME} --image-ids imageDigest=\${amd64_sha} --region ${REGION_NAME} --output json | jq --raw-output --join-output '.images[0].imageManifest')
            aws ecr put-image --repository-name ${REPOSITORY_NAME} --image-tag \${AMD_TAG} --image-manifest "\${MANIFEST_AMD}" --region ${REGION_NAME}
            MANIFEST_ARM=\$(aws ecr batch-get-image --repository-name ${REPOSITORY_NAME} --image-ids imageDigest=\${arm64_sha} --region ${REGION_NAME} --output json | jq --raw-output --join-output '.images[0].imageManifest')
            aws ecr put-image --repository-name ${REPOSITORY_NAME} --image-tag \${ARM_TAG} --image-manifest "\${MANIFEST_ARM}" --region ${REGION_NAME}
        """
    
}
// setting env. variables
def setupEnvironments(BUILD_NUMBER,COMMIT_ID,DEFAULT_ENV,DEFAULT_PROJECT_PREFIX,MODULE,REPOSITORY_NUMBER,REGION_NAME,ENV,BRANCH_NAME,IMAGE_NAME,DOCKER_IMAGE_TAG,EKS_IMAGE_TAG,EKS_PREFIX) {
    sh '''
    pwd
    ls -la
    '''
    def COMMIT_ID = sh(returnStdout: true, script: "git rev-parse --short HEAD").trim()
    def ARTIFACT_VERSION = "${BUILD_NUMBER}-${COMMIT_ID}"

    def REPOSITORY_NAME = "${DEFAULT_ENV}-${DEFAULT_PROJECT_PREFIX}-${MODULE}"
    def IMAGE_NAME = "${REPOSITORY_NUMBER}.dkr.ecr.${REGION_NAME}.amazonaws.com/${REPOSITORY_NAME}"

    def DOCKER_IMAGE_TAG = "${ENV}_${BRANCH_NAME}_${COMMIT_ID}"
    def EKS_IMAGE_TAG = "${ENV}_latest"

    def DOCKER_IMAGE_NAME = "${IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
    def EKS_IMAGE_NAME = "${IMAGE_NAME}:${EKS_IMAGE_TAG}"

    def CLUSTER_NAME = "${ENV}-${env.DEFAULT_PROJECT_PREFIX}"
    def EKS_PREFIX = "${ENV}-${env.DEFAULT_PROJECT_PREFIX}-${MODULE}"
    def TASK_NAME = "${EKS_PREFIX}"
    def SERVICE_NAME = "${EKS_PREFIX}"

    return [
        ARTIFACT_VERSION: ARTIFACT_VERSION,
        REPOSITORY_NAME: REPOSITORY_NAME,
        IMAGE_NAME: IMAGE_NAME,
        DOCKER_IMAGE_NAME: DOCKER_IMAGE_NAME,
        EKS_IMAGE_NAME: EKS_IMAGE_NAME,
        CLUSTER_NAME: CLUSTER_NAME,
        TASK_NAME: TASK_NAME,
        SERVICE_NAME: SERVICE_NAME
    ]
}



