pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Install Docker') {
            steps{
                sh "ansible-playbook -i ~/workspace/TIS-OPS/ansible/inventory/simple-inventory.ini ~/workspace/TIS-DEVOPS/ansible/tasks/docker-upgrade.yml"
            }
        }
             
    }
}
