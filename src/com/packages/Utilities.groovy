

// helm deploy
def helmInstall(String namespace, String release, String module, String dockerImageTag, String KUBE_CONFIG) {
    echo "Installing ${release} in ${namespace}"
    script {
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
}


// clean workspace
def cleanWorkspace() {
    step([$class: 'WsCleanup'])
}

// clone repository
def pullRepository(String branch, String env) {
    echo "Branch: ${branch}"
    echo "Environment: ${env}"
    
    script {
        def BRANCH_NAME = branch ?: env.DEFAULT_BRANCH
        
        git branch: "${BRANCH_NAME}", url: "${env.GIT_URL}", changelog: true, poll: true
    }
}    

