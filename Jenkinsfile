pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Step-1') {
            steps{
                sh "pwd"
            }
        }
        
        stage ('Step-2') {
            steps{
                sh "ansible-playbook -i ~/workspace/TIS-OPS/ansible/inventory/simple-inventory.ini ~/workspace/TIS-OPS/ansible/install-docker.yml -vvv"
            }
        }
             
    }
}
