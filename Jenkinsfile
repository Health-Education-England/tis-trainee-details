node {

  deleteDir()

  stage('Checkout Git Repo') {
    checkout scm
  }

  env.GIT_COMMIT=sh (script: "git log -n 1 --pretty=format:'%H'", returnStdout: true)
  def workspace = pwd()
  def parent_workspace = pwd()
  def repository = "${env.GIT_COMMIT}".split("TIS-")[-1].split(".git")[0]
  def buildNumber = env.BUILD_NUMBER
  def buildVersion = env.GIT_COMMIT
  def imageName = ""
  def imageVersionTag = ""
  def jobName = "${env.JOB_NAME}"
  def buildStatus = currentBuild.result == null ? "Success" : currentBuild.result

  println "[Jenkinsfile INFO] Commit Hash is ${GIT_COMMIT}"

    milestone 1

     stage('test') {      
        sh 'scp $env.DEVOPS_BASE/README.md ubuntu@172.26.1.140:readme'
    }
  
}
