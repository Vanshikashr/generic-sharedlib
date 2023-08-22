#!/usr/bin/groovy

import com.packages.Utilities
import com.packages.Build
import com.packages.Deploy
def call( ENV ,  BRANCH_NAME, MODULE,  DEFAULT_PROJECT_PREFIX,  REGION_NAME, REPOSITORY_NUMBER,  GIT_URL,  S3_BUCKET_NAME, S3_BUCKET_PATH,  HELM_REPO, HELM_BRANCH,  KUBE_CONFIG)
{
    build = new Build()
    utilities = new Utilities()
    deploy = new Deploy()
