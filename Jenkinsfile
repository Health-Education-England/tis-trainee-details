pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Test') {
            steps{
                sh 'scp file1 ubuntu@172.26.1.140:file1-received'
            }
        }
             
    }
}
