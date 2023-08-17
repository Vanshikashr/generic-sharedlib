// deploy
def deployApp(String helmBranch, String helmRepo, String env, String module, String dockerImageTag, String kubeConfig) {
    script {
        git branch: "${helmBranch}", url: "${helmRepo}", changelog: true, poll: true
        helmInstall(env, "${env}-${module}", module, dockerImageTag, kubeConfig)
    }
}

