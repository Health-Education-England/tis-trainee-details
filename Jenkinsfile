pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Test') {
            steps{
                sh 'scp DEVOPS_BASE/README.md ubuntu@172.26.1.140:readme'
            }
        }
        
        
        
    }
}
