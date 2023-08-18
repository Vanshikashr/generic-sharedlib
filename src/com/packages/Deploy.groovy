// deploy
def deployApp() {

        git branch: "${HELM_BRANCH}", url: "${HELM_REPO}", changelog: true, poll: true
        helmInstall("${ENV}, "${ENV}-${MODULE}", MODULE, DOCKER_IMAGE_TAG, KUBE_CONFIG )
    
}

