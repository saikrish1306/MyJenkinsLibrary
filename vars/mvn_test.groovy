// vars/mvnTest.groovy
def call(Map config = [:]) {
   
        stages {
            stage('Unit Tests') {
                steps {
                    script {
                        echo "Running Unit Tests..."
                        sh "mvn clean test -Dsurefire.rerunFailingTestsCount=2 -Dparallel=methods -DthreadCount=4"
                    }
                }
            }

            stage('Integration Tests') {
                when { expression { config.runIntegrationTests ?: false } }  // Run only if enabled
                steps {
                    script {
                        echo "Running Integration Tests..."
                        sh "mvn verify -DskipUnitTests=true"
                    }
                }
            }
        }

        post {
            success {
                echo "Tests Passed Successfully!"
            }
            failure {
                echo "Tests Failed!"
                script {
                    notifySlack("Tests Failed for ${env.JOB_NAME}:${env.BUILD_NUMBER}")
                }
            }
            always {
                echo "Test Execution Completed."
            }
        }
    }
}
