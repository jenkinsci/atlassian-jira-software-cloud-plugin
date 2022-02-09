pipeline {
    agent  any
    stages {
        stage('build') {
            steps {
                sleep 1
                echo  'done build'
            }
        }
    }
}