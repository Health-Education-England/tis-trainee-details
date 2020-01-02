pipeline {
    agent { label 'master' }
    stages {
        
        stage ('Test') {
            steps{
                sh "ansible-playbook -vvv -i /var/jenkins_home/workspace/TIS-OPS/ansible/inventory/simple-inventory.ini /var/jenkins_home/workspace/TIS-OPS/ansible/install-docker.yml"
            }
        }
             
    }
}
