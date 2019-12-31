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
                    sh 'ssh -o StrictHostKeyChecking=no ubuntu@172.26.1.140 uptime'
                    sh 'ssh -v ubuntu@172.26.1.140'
                    sh 'scp test2  ubuntu@172.26.1.140:test2.txt'
                }
            }
        }
        
    }
}
