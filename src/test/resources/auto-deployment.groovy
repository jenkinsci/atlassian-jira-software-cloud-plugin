pipeline {
    agent  any
    stages {
        stage('deployments') {
            parallel {
                stage('deploy to stg') {
                    steps {
                        echo 'stg done deployment'
                    }
                }
                stage('deploy to prod') {
                    steps {
                        echo 'stg done deployment'
                    }
                }
            }
        }
    }
}