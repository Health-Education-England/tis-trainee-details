pipeline {
    agent { label 'master' }
    stages {
        
        stage('build') {
            steps {
                echo "testing credentials"
            }
        }
        
        stage ('Deploy') {
            steps{
                sshagent(credentials : ['jenkins']) {
                    sh 'ssh -o StrictHostKeyChecking=no ubuntu@172.26.1.140 ls'
                }
            }
        }
        
    }
}
