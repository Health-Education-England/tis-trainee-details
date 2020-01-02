pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Deploy') {
            steps{
                sshagent(credentials : ['jenkins']) {
                    sh 'ssh -o StrictHostKeyChecking=no ubuntu@172.26.1.140 ls'
                    sh 'ls'
                }
            }
        }
        
    }
}
