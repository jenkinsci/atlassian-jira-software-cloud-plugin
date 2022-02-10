pipeline {
    agent  any
    stages {
        stage('build') {
            steps {
                sleep 1
                echo  'done build'
            }
        }
        stage('deployments') {
            stages {
                stage('deploy to stg') {
                    steps {
                        echo 'stg deployment done'
                    }
                }
                stage('do something else') {
                    steps {
                        echo 'doing something else'
                    }
                }
                stage('deploy to prod') {
                    steps {
                        echo 'failing'
                        exit 1
                    }
                }
            }
        }
    }
}