pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Test') {
            steps{
                sh 'ls'
            }
        }
        
        stage ('Deploy') {
            steps{
                sshagent(credentials : ['jenkins']) {
                    sh 'ssh -o StrictHostKeyChecking=no ubuntu@172.26.1.140'
                }
            }
        }
        
    }
}
