// deploy
def deployApp(String HELM_BRANCH, String HELM_REPO, String ENV, String MODULE , String DOCKER_IMAGE_TAG, String kubeConfig) {

        git branch: "${HELM_BRANCH}", url: "${HELM_REPO}", changelog: true, poll: true
        helmInstall("${ENV}, "${ENV}-${MODULE}", MODULE, DOCKER_IMAGE_TAG, KUBE_CONFIG )
    
}

