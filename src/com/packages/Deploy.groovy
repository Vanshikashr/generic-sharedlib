package com.packages
//  ECS deploy on existing cluster
//==============================================
//This function  deploy latest version of task-definition
def ecsDeploy() {
  def taskDefinition = env.taskDefinition != 'null' ? env.taskDefinition : env.appName
      if(taskJq()){
  def clusterName = env.clusterName 
  def serviceName = env.serviceName != 'null' ? env.serviceName : env.appName
  def Imagename = "${containerRegistoryUrl}/${appName}:${dockerImageTag}"
  def TASK_DEFINITION = sh(script: "aws ecs describe-task-definition --task-definition '${taskDefinition}'", returnStdout: true).trim()
  def NEW_TASK_DEFINITION = sh(script: "echo '${TASK_DEFINITION}' | jq --arg IMAGE '${Imagename}' '.taskDefinition | .containerDefinitions[0].image = \$IMAGE | del(.taskDefinitionArn) | del(.revision) | del(.status) | del(.requiresAttributes) | del(.compatibilities) | del(.registeredAt) | del(.registeredBy)'", returnStdout: true).trim()
  def NEW_TASK_INFO = sh(script: "aws ecs register-task-definition --cli-input-json '${NEW_TASK_DEFINITION}'", returnStdout: true).trim()
  def NEW_REVISION = sh(script: "echo '${NEW_TASK_INFO}' | jq '.taskDefinition.revision'", returnStdout: true).trim()
  sh "aws ecs update-service --cluster ${clusterName} --service ${serviceName} --task-definition ${taskDefinition}:${NEW_REVISION}"
}
}
