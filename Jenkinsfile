
pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot'
        MAVEN_HOME = 'C:/Users/Asus/apache-maven-3.9.16-bin/apache-maven-3.9.16'
        DOCKER_IMAGE = 'jaisshaurya/student-placement-portal'
        K8S_DEPLOYMENT = 'student-placement-portal-app'
        K8S_CONTAINER = 'student-placement-portal'
        K8S_SERVICE = 'student-placement-portal-app-service'
    }

    stages {
        stage('Verify Environment') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    Write-Host "===== JAVA VERSION ====="
                    & "$env:JAVA_HOME\\bin\\java.exe" -version 2>&1 |
                        ForEach-Object { Write-Host $_ }

                    if ($LASTEXITCODE -ne 0) {
                        throw "Java verification failed."
                    }

                    Write-Host "===== MAVEN VERSION ====="
                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" -version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven verification failed."
                    }

                    Write-Host "===== DOCKER ENGINE ====="
                    & docker version
                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot access Docker Engine."
                    }

                    Write-Host "Environment verification passed."
                '''
            }
        }

        stage('Build and Test') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    if (-not (Test-Path "pom.xml")) {
                        throw "pom.xml not found in the workspace."
                    }

                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" clean test package

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven build or tests failed."
                    }

                    if (-not (Test-Path "target/student-placement-portal-1.0.0.jar")) {
                        throw "Executable JAR was not generated."
                    }

                    Write-Host "Build and test stage succeeded."
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    if (-not (Test-Path "Dockerfile")) {
                        throw "Dockerfile not found."
                    }

                    $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                    $latestTag = "$env:DOCKER_IMAGE`:latest"

                    Write-Host "Building $imageTag"

                    & docker build -t $imageTag -t $latestTag .

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image build failed."
                    }

                    Write-Host "Docker image build succeeded."
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DH_USER',
                        passwordVariable: 'DH_TOKEN'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = 'Stop'
                        $dockerConfig = Join-Path $env:WORKSPACE ".docker-config-$env:BUILD_NUMBER"

                        try {
                            New-Item -ItemType Directory `
                                -Path $dockerConfig -Force | Out-Null

                            $env:DOCKER_CONFIG = $dockerConfig

                            Write-Host "Authenticating to Docker Hub..."

                            $env:DH_TOKEN | & docker login `
                                --username "$env:DH_USER" `
                                --password-stdin

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub authentication failed."
                            }

                            $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                            $latestTag = "$env:DOCKER_IMAGE`:latest"

                            Write-Host "Pushing versioned image..."
                            & docker push $imageTag

                            if ($LASTEXITCODE -ne 0) {
                                throw "Versioned image push failed."
                            }

                            Write-Host "Pushing latest image..."
                            & docker push $latestTag

                            if ($LASTEXITCODE -ne 0) {
                                throw "Latest image push failed."
                            }

                            Write-Host "Docker Hub publishing succeeded."
                        }
                        finally {
                            & docker logout 2>$null | Out-Null

                            Remove-Item -Recurse -Force $dockerConfig `
                                -ErrorAction SilentlyContinue

                            Remove-Item Env:DOCKER_CONFIG `
                                -ErrorAction SilentlyContinue
                        }
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'kubeconfig-global',
                        variable: 'KUBECONFIG_FILE'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = 'Stop'
                        $env:KUBECONFIG = $env:KUBECONFIG_FILE

                        $manifestPath = Join-Path $env:WORKSPACE "deployment-ci.yaml"

                        try {
                            Write-Host "===== KUBERNETES CONTEXT ====="

                            $context = (& kubectl config current-context).Trim()

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not read Kubernetes context."
                            }

                            if ($context -ne "docker-desktop") {
                                throw "Expected docker-desktop context; found '$context'."
                            }

                            & kubectl cluster-info

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes cluster is unreachable."
                            }

                            if (-not (Test-Path "deployment.yaml")) {
                                throw "deployment.yaml not found."
                            }

                            $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                            $manifest = Get-Content "deployment.yaml" -Raw

                            $originalImage = "jaisshaurya/student-placement-portal:latest"

                            if (-not $manifest.Contains($originalImage)) {
                                throw "Expected image placeholder not found in deployment.yaml."
                            }

                            $manifest = $manifest.Replace($originalImage, $imageTag)

                            Set-Content -Path $manifestPath `
                                -Value $manifest -Encoding ascii

                            Write-Host "Applying Kubernetes manifest..."
                            & kubectl apply -f $manifestPath

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes manifest apply failed."
                            }

                            Write-Host "Waiting for deployment rollout..."
                            & kubectl rollout status `
                                "deployment/$env:K8S_DEPLOYMENT" `
                                --timeout=180s

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes rollout failed."
                            }

                            Write-Host "===== DEPLOYMENT STATUS ====="
                            & kubectl get deployment $env:K8S_DEPLOYMENT

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not inspect deployment."
                            }

                            & kubectl get pods `
                                -l "app=$env:K8S_DEPLOYMENT" -o wide

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not inspect application pods."
                            }

                            & kubectl get service $env:K8S_SERVICE

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not inspect application service."
                            }
                        }
                        finally {
                            Remove-Item $manifestPath -Force `
                                -ErrorAction SilentlyContinue

                            Remove-Item Env:KUBECONFIG `
                                -ErrorAction SilentlyContinue
                        }
                    '''
                }
            }
        }

        stage('Verify Application') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $url = 'http://localhost:30085/api/health'
                    $healthy = $false

                    for ($attempt = 1; $attempt -le 12; $attempt++) {
                        try {
                            $response = Invoke-WebRequest `
                                -Uri $url `
                                -TimeoutSec 5 `
                                -UseBasicParsing

                            if ($response.StatusCode -eq 200) {
                                Write-Host "Application health check passed."
                                Write-Host $response.Content
                                $healthy = $true
                                break
                            }
                        }
                        catch {
                            Write-Host "Health check attempt $attempt failed."
                        }

                        Start-Sleep -Seconds 5
                    }

                    if (-not $healthy) {
                        throw "Application did not return HTTP 200 at $url"
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
                    echo 'No Maven test reports were produced.'
                }

                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true,
                                 allowEmptyArchive: true
            }
        }

        success {
            echo 'CI/CD succeeded: build, tests, Docker Hub push, Kubernetes deployment and health check.'
        }

        failure {
            echo 'CI/CD failed. Inspect the first failing stage in Console Output.'
        }
    }
}
