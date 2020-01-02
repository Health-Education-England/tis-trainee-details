// -*- mode: groovy -*-
// vim: set filetype=groovy :

@Library('utils@master')_

def utils = new hee.tis.utils()

def service = "trainee-details"

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

  try {

    milestone 1

     stage('test') {
      try {
        sh 'scp $env.DEVOPS_BASE/README.md ubuntu@172.26.1.140:readme'
      } catch (err) {
        throw err
      }
    }

   
        

    }
  } catch (hudson.AbortException ae) {
    // We want to do nothing for Aborts.
  } catch (err) {
    throw err
  } finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: 'npm-debug.log, package-lock.json, src/*'
  }
}
