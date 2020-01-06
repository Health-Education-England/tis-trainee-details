pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Install Docker') {
            steps{
                sh "ansible-playbook -i ~/workspace/TIS-OPS/ansible/inventory/simple-inventory.ini ~/workspace/TIS-OPS/ansible/install-docker.yml"
            }
        }
             
    }
}
