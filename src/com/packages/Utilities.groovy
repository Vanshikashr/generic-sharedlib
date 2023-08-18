

// helm deploy
def helmInstall(String namespace, String release, String module, String dockerImageTag, String KUBE_CONFIG) {
    echo "Installing ${release} in ${namespace}"
   
        sh """#!/bin/bash
            set -xe
            cd $workspace/services
            ls
            aws eks --region ap-south-1 update-kubeconfig --kubeconfig=${KUBE_CONFIG} --name non-prod-eupi-eks
            /usr/local/bin/helm upgrade --install --namespace ${namespace} --kubeconfig=${KUBE_CONFIG} ${release} --set image.tag=${dockerImageTag} -f ./${module}/values-${namespace}.yaml ./${module}
            /root/bin/kubectl rollout status deployment ${module} --namespace ${namespace} --kubeconfig=${KUBE_CONFIG}
            sleep 120s
        """
    
}


// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}

// clone repository
def pullRepository(String branch, String env) {
    echo "Branch: ${branch}"
    echo "Environment: ${env}"
    
    
        def BRANCH_NAME = branch ?: env.DEFAULT_BRANCH
        
        git branch: "${BRANCH_NAME}", url: "${env.GIT_URL}", changelog: true, poll: true
    
}   

// docker push
def dockerImagePush(String workspace, String regionName, String repositoryNumber, String dockerImageName, String eksImageName, String commitId, String dockerImageTag, String repositoryName) {

        sh """#!/bin/bash
            set -xe
            echo $workspace

            echo "Docker Image Push"
            aws ecr get-login-password --region ${regionName} | docker login --username AWS --password-stdin ${repositoryNumber}.dkr.ecr.${regionName}.amazonaws.com
            docker rmi -f ${dockerImageName}
            docker buildx build --platform linux/amd64,linux/arm64 --provenance=false -f Dockerfile --build-arg artifact_version=${commitId} -t ${dockerImageName} -t ${eksImageName} --push .

            if [ \$? -eq 0 ]
            then
                echo "Successfully image tagged and pushed to repository"
                echo ${dockerImageName} > $workspace/image_id
                cat $workspace/image_id
            else
                echo "Error in tagging/pushing image"
                exit 1
            fi

            MANIFEST_LIST=`docker manifest inspect ${dockerImageName}`

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

            AMD_TAG="${dockerImageTag}-amd"
            ARM_TAG="${dockerImageTag}-arm"

            MANIFEST_AMD=\$(aws ecr batch-get-image --repository-name ${repositoryName} --image-ids imageDigest=\${amd64_sha} --region ${regionName} --output json | jq --raw-output --join-output '.images[0].imageManifest')
            aws ecr put-image --repository-name ${repositoryName} --image-tag \${AMD_TAG} --image-manifest "\${MANIFEST_AMD}" --region ${regionName}
            MANIFEST_ARM=\$(aws ecr batch-get-image --repository-name ${repositoryName} --image-ids imageDigest=\${arm64_sha} --region ${regionName} --output json | jq --raw-output --join-output '.images[0].imageManifest')
            aws ecr put-image --repository-name ${repositoryName} --image-tag \${ARM_TAG} --image-manifest "\${MANIFEST_ARM}" --region ${regionName}
        """
    
}
// setting env. variables
def setupEnvironments(String env, String branch, String module) {
    def COMMIT_ID = sh(returnStdout: true, script: "git rev-parse --short HEAD").trim()
    def ARTIFACT_VERSION = "${BUILD_NUMBER}-${COMMIT_ID}"

    def REPOSITORY_NAME = "${env.DEFAULT_ENV}-${env.DEFAULT_PROJECT_PREFIX}-${module}"
    def IMAGE_NAME = "${env.REPOSITORY_NUMBER}.dkr.ecr.${env.REGION_NAME}.amazonaws.com/${REPOSITORY_NAME}"

    def DOCKER_IMAGE_TAG = "${env}_${branch}_${COMMIT_ID}"
    def EKS_IMAGE_TAG = "${env}_latest"

    def DOCKER_IMAGE_NAME = "${IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
    def EKS_IMAGE_NAME = "${IMAGE_NAME}:${EKS_IMAGE_TAG}"

    def CLUSTER_NAME = "${env}-${env.DEFAULT_PROJECT_PREFIX}"
    def EKS_PREFIX = "${env}-${env.DEFAULT_PROJECT_PREFIX}-${module}"
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



