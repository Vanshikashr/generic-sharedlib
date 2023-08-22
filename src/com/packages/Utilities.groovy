package com.packages
import com.packages.Deploy

//===================================
//        App name generator
//===================================
// This function sets env variable name `appName`
def generateAppName(){
  if(env.appName == "null"){	  
  env.appName = sh(script: "cat .git/config | grep -i 'url' | rev | cut -d / -f 1 | cut -d . -f 2 | rev" , returnStdout: true).trim()
    println("App Name: "+ env.appName)
  }
  else{
    def appName = env.appName
  }
}



//gitTag through choice parameters
def gitTag() {
params.gitRepoUrl ?: error('gitRepoUrl parameter not passed.')
params.releaseType ?: error('releaseType parameter not passed.')
try {
 // Get all references (tags and branches)
def references = sh(returnStdout: true, script: "git ls-remote --tags ${gitRepoUrl}")
// Process the references to extract tags only
def tags = references.readLines().findAll { it.endsWith('^{}') }.collect { it.split('/')[2] }
// Convert tags to a string
def tagsAsString = tags.join('\n')
// Split the tags into an array
def tagArray = tagsAsString.split('\n')
// Print the tags
println("Tags:")
tagArray.each { tag ->
 println("- $tag")
        }
// Get the latest tag (first element)
 def latestTag = tagArray.sort().last()
 // Remove the non-numeric part ("v") from the tag
  latestTag = latestTag.replaceAll(/[^0-9.]/, '') 
  println("Latest tag: $latestTag")
  def versionParts = latestTag.split("\\.")
  def majorVersion = versionParts[0]
  def minorVersion = versionParts[1]
  def hotfixVersion = versionParts[2]
 println "Release Type: $releaseType"
 println "Major version: $majorVersion"
 println "Minor version: $minorVersion"
 println "Hotfix version: $hotfixVersion"
 switch (releaseType) {
  case "major":
   majorVersion = (majorVersion as int) + 1
   minorVersion = 0
   hotfixVersion = 0
   break
   case "minor":
   minorVersion = (minorVersion as int) + 1
   hotfixVersion = 0
   break
   case "hotfix":
   hotfixVersion = (hotfixVersion as int) + 1
   break
   default:
   println "Invalid option selected."
   }
  latestTag = "v${majorVersion}.${minorVersion}.${hotfixVersion}"
  println "Updated latestTag: $latestTag"
   } catch (Exception e) {
   println("Error occurred: ${e.getMessage()}")
   return null
    }
}
//====================================
//      Sonarqube test
//====================================
//This function analyses the code quality and coverages
def sonarQubeTest() {
    
    // Execute SonarScanner analysis using curl command
    sh """
        curl -X POST \\
        -u ${sonarqubeUsername}:${sonarqubePassword} \\
        -H 'Content-Type: application/x-www-form-urlencoded' \\
        -d 'project=${projectToken}&name=${projectName}' \\
        ${sonarqubeUrl}/api/qualitygates/project_status
    """

    // Fetch SonarQube metrics using curl command
    def metricsResponse = sh(script: """
        curl -s -u ${sonarqubeUsername}:${sonarqubePassword} \\
        -H 'Content-Type: application/json' \\
        "${sonarqubeUrl}/api/measures/component?component=${projectToken}&metricKeys=code_smells,vulnerabilities,bugs"
    """, returnStdout: true).trim()

    // Check if the curl command returned an error
    if (metricsResponse.contains("error")) {
        println "Failed to fetch SonarQube metrics."
        return
    }

    // Parse the metrics values
    def jsonSlurper = new groovy.json.JsonSlurper()
    def metricsMap
    try {
        metricsMap = jsonSlurper.parseText(metricsResponse)
    } catch (Exception e) {
        println "Error parsing JSON response: ${e.getMessage()}"
        return
    }

    // Check if the required fields are present
    def codeSmells = metricsMap?.component?.measures?.code_smells?.value ?: 0
    def vulnerabilities = metricsMap?.component?.measures?.vulnerabilities?.value ?: 0
    def bugs = metricsMap?.component?.measures?.bugs?.value ?: 0

    // Calculate the current success percentage
    def currentPercentage = 100 - ((codeSmells + vulnerabilities + bugs) / 3)

    // Compare with the previous percentage and fail the job if the current percentage is lower
    def previousPercentage = 0
    def previousPercentageFilePath = "/var/lib/jenkins/sonarqubereport/previous_percentage.txt"

    // Check if the previous percentage file exists
    if (fileExists(previousPercentageFilePath)) {
        try {
            previousPercentage = Integer.parseInt(readFile(file: previousPercentageFilePath).trim())
        } catch (NumberFormatException e) {
            println "Error reading previous percentage from file: ${e.getMessage()}"
        }
    } else {
        println "Previous percentage file not found. Assuming previous percentage as 0."
    }

    println "Previous Success Percentage: $previousPercentage%"
    println "Current Success Percentage: $currentPercentage%"

    if (currentPercentage < previousPercentage) {
        println "Current success percentage is lower than the previous percentage. Job failed."
        return
    }

    // Update the previous success percentage file with the current percentage
    writeFile file: previousPercentageFilePath, text: currentPercentage.toString(), encoding: 'UTF-8'

    // Generate the HTML report
    def htmlReport = """
        <html>
        <head>
            <title>SonarQube Metrics Report</title>
        </head>
        <body>
            <h1>SonarQube Metrics Report</h1>
            <h2>Code Smells: ${codeSmells ?: 'N/A'}</h2>
            <h2>Vulnerabilities: ${vulnerabilities ?: 'N/A'}</h2>
            <h2>Bugs: ${bugs ?: 'N/A'}</h2>
            <h2>Success Percentage: ${currentPercentage}%</h2>
            <!-- Include other metrics as desired -->
        </body>
        </html>
    """

    // Convert HTML to PDF
    def pdfFilePath = "/var/lib/jenkins/sonarqubereport/metrics_report.pdf"
    writeFile file: pdfFilePath, text: htmlReport, encoding: 'UTF-8'

    // Print the location of the PDF report
    println "Metrics report converted to PDF: ${pdfFilePath}"
}


//====================================
//		  Notification Type
//====================================
//This function check which type notification(gchat,email,slack) will run 
def notificationType(notification) {
notification.each { item ->
 println("${item.type}")
  switch ("${item.type}") {
    case "slack":
      println("${item.type}")
      slackNotification(item)
      break

    case "gchat":
      println("${item.type}")
      googleChatNotification(item)
      break

    case "email":
      emailNotification(item)
      break

    default:
      println("Unknown notification type: ${item.type}")
      break
  }
}
       
        

}



//=============================================
//               EmailNotification
//=============================================
//This function Notify about Success ,Failure , and Abort for pipeline Jobs Using Slack. 
def emailNotification(item)
{
   def fromAddress = item.fromAddress
   def toAddress = item.toAddress
  node {
  wrap([$class: 'BuildUser']) {
    def user = env.BUILD_USER_ID
    def jobName = env.JOB_NAME
   def awsCliCommand = "aws ses send-email --from ${fromAddress} --to ${toAddress} --subject 'Job Status:  (${user}): ${currentBuild.currentResult}: ${jobName}'"
    if (currentBuild.currentResult == 'SUCCESS') {
       awsCliCommand += " --text 'The job was successful.'"
    } else {
       awsCliCommand += " --text 'The job has failed.'"
    }
  // Execute the AWS CLI command
    sh awsCliCommand
    }
}
}

//====================================
//     git Clone
//====================================
    def gitClone(){
    def branchName =  env.branchName
    def jenkinsSecret = env.jenkinsSecret
    def repoLink = env.repoLink
    git branch: "${branchName}", credentialsId: "${jenkinsSecret}", url: "${repoLink}"
}


//====================================
//		  Image tag generator
//====================================
//This function generates image tag name based on git commit id.
//Prerequisites - 
//	1. function should be excuted in git repo directory.
//	2. git cli should be installed on server.
def generateImageTag(){
	failed = sh (returnStatus: true, script: 'command -v git')
	if (failed){
        error 'git command does not exist. Please install.'
    }
    else {
		failed = sh (returnStatus: true, script: 'command -v git')
		if (failed){
			error 'Not git repo.'
		}else{
		        def gitCommitId = sh(script: "printf \$(git rev-parse --short HEAD)", returnStdout: true)
                        def timestamp = sh(script: 'date +%s', returnStdout: true).trim()
			env.dockerImageTag = "${gitCommitId}-${timestamp}"
		}
	}
}


//===================================
//    Clean unused images
//===================================
//This function cleans docker images on server.
def cleanupDockerImage() {
    sh 'docker system prune -af'
}


//function for loading config.yaml file
def loadConfig(String configResourcePath, String configAppPath) {
     def configResourceData = libraryResource("${configResourcePath}")
     def configResourceJson =  readYaml text: configResourceData
     def config
     if (fileExists("${configAppPath}")){
        def configAppJson = readYaml file: "${WORKSPACE}/${configAppPath}"
        config = configResourceJson + configAppJson
     }
     else {
        config = configResourceJson
     }
     return config
     }



//====================================
//          Docker ECR login
//====================================
//This function logs into ECR repo
// prameters:-
//	> containerRegistoryUrl - docker repo url (format: <accountName>.dkr.ecr.<region>.amazonaws.com)
def dockerLoginEcr(containerRegistoryUrl,awsRegion) {  
    def containerRegistoryUrl = env.containerRegistoryUrl
    env.awsRegion = sh(script: "echo ${containerRegistoryUrl} | cut -d . -f 4" , returnStdout: true).trim()
    println('Logging into ECR repository...')
    failed = sh (returnStatus: true, script: 'command -v aws')
    if (failed){
        error 'aws command does not exist. Please install.'
    }
    else {
        sh "aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${containerRegistoryUrl}"
    }
}


//=============================================
//                SlackNotification
//=============================================
//This function Notify about Success ,Failure , and Abort for pipeline Jobs Using Slack. 

def getBuildUser() {
    return currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
}
  

def slackNotification(item)
{
  def COLOR_MAP = [
      "SUCCESS": 'good',
      "FAILURE": 'danger',
      "ABORTED": 'warning'
      ]
 BUILD_USER = getBuildUser()
    def slackWebhookUrl = item.webhookUrl
    def message= "*${currentBuild.currentResult}:*\n *AppName:* ${env.app_name}\n *Job_Name:* ${env.JOB_NAME}\n *Build_Number:* ${env.BUILD_NUMBER} \n *Build By* ${BUILD_USER}\n *More info at:* ${env.BUILD_URL}"
    sh  """
    curl -X POST -H 'Content-type: application/json'  --data '{ \"attachments\": [{ \"color\": \"${COLOR_MAP[currentBuild.currentResult]}\", \"text\": \"${message}\" }] }' ${slackWebhookUrl}
    """
}


//=============================================
//                GoogleChatNotification
//=============================================
//This function Notify about Success ,Failure , and Abort for pipeline Jobs Through Googlechatnotification. 


def googleChatNotification(item){
   BUILD_USER = getBuildUser()
    def message= "*${currentBuild.currentResult}:*\n *AppName:* ${env.app_name}\n*Job_Name:* ${env.JOB_NAME}\n *Build_Number:* ${env.BUILD_NUMBER} \n *Build By* ${BUILD_USER}\n *More info at:* ${env.BUILD_URL}"
    def gchatWebhookUrl = item.webhookUrl
    sh """ curl -XPOST -H 'Content-Type: application/json; charset=UTF-8' -d "{"'"text"'": "'"${message}"'"}" "${gchatWebhookUrl}"
    """
}


//Clone repo
def gitClone(String jenkinsSecret, String repoLink, String branchName="main") {
    git branch: "${branchName}", credentialsId: "${jenkinsSecret}", url: "${repoLink}"
}


//=============================================
//                Push docker repo
//=============================================
//This function push docker image in its repo. Assuming it is named appropriately. 
def dockerPush() {
  def containerRegistoryUrl = env.containerRegistoryUrl
  def appName = env.appName
  branchName = "$GIT_BRANCH" 
  sh "docker push ${containerRegistoryUrl}/${appName}:${dockerImageTag}"
  sh "docker push ${containerRegistoryUrl}/${appName}:${branchName}-latest"
}
