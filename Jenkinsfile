pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Step-1') {
            steps{
                sh "ls ~/workspace/TIS-OPS/ansible"
            }
        }
        
        stage ('Step-2') {
            steps{
                sh "scp test21 ubuntu@3.9.173.95:test21-received"
            }
        }
        
        stage ('Step-3') {
            steps{
                sh "ansible-playbook -i ~/workspace/TIS-OPS/ansible/inventory/simple-inventory.ini ~/workspace/TIS-OPS/ansible/install-docker.yml -vvv"
            }
        }
             
    }
}
