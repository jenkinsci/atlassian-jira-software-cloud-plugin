pipeline {
    agent  any
    stages {
        stage('build') {
            steps {
                exit 1
            }
        }
    }
}