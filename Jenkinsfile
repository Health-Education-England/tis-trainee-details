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
                    sh "ssh ubuntu@3.9.180.246 echo 'test remote'"
                }
            }
        }
        
    }
}
