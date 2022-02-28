pipeline {
    agent  any
    stages {

        stage('doing something else (not triggering a build event)') {
            steps {
                echo  'doing something else'
            }
        }

        stage('build 1 (triggering a build event)') {
            steps {
                echo  'done build 1'
            }
        }

        stage('build 2 (triggering a build event)') {
            steps {
                echo  'done build 2'
            }
        }

        stage('doing something else again (not triggering a build event)') {
            steps {
                echo  'doing something else'
            }
        }
    }
}