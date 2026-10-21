pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot'
        MAVEN_HOME = 'C:/Users/Asus/apache-maven-3.9.16-bin/apache-maven-3.9.16'

        PATH+JAVA = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot/bin'
        PATH+MAVEN = 'C:/Users/Asus/apache-maven-3.9.16-bin/apache-maven-3.9.16/bin'
    }

    stages {
        stage('Verify Environment') {
            steps {
                powershell '''
                    Write-Host "===== JAVA ====="
                    java -version

                    Write-Host "===== MAVEN ====="
                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" -version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven verification failed."
                    }
                '''
            }
        }

        stage('Build and Test') {
            steps {
                powershell '''
                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" clean test package

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven build or tests failed."
                    }
                '''
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml',
                  allowEmptyResults: false

            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true,
                             allowEmptyArchive: false
        }

        success {
            echo 'Student Placement Portal build and tests succeeded.'
        }

        failure {
            echo 'Build failed. Review the Jenkins console output.'
        }
    }
}
