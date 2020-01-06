pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Install Docker') {
            steps{
                sh "ansible-playbook -i ~/workspace/TIS-DEVOPS/ansible/inventory/simple-inventory.ini ~/workspace/TIS-DEVOPS/ansible/tasks/docker-upgrade.yml"
            }
        }
             
    }
}
