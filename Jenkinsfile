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
                    sh "ssh ubuntu@172.26.1.140 echo 'test remote'"
                }
            }
        }
        
    }
}
