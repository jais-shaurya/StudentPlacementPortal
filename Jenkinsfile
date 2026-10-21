
pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot'
        MAVEN_HOME = 'C:/Users/Asus/apache-maven-3.9.16-bin/apache-maven-3.9.16'
    }

    stages {
        stage('Verify Environment') {
            steps {
                powershell '''
                    Write-Host "===== JAVA VERSION ====="
                    & "$env:JAVA_HOME\\bin\\java.exe" -version 2>&1 |
                        ForEach-Object { Write-Host $_ }

                    Write-Host "===== MAVEN VERSION ====="
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
                    Write-Host "===== PROJECT DIRECTORY ====="

                    if (-not (Test-Path "pom.xml")) {
                        throw "pom.xml was not found in the workspace."
                    }

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
            script {
                if (fileExists('target/surefire-reports')) {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true
                } else {
                    echo 'No test reports available.'
                }

                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true,
                                 allowEmptyArchive: true
            }
        }

        success {
            echo 'Student Placement Portal build and tests succeeded.'
        }

        failure {
            echo 'Build failed. Review the Console Output.'
        }
    }
}
