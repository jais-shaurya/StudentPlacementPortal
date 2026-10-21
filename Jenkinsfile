cd C:\Projects\DevOps\StudentPlacementPortal

@'
pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
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

                    Write-Host "===== JAVA ====="
                    & "$env:JAVA_HOME\\bin\\java.exe" -version 2>&1 |
                        ForEach-Object { Write-Host $_ }

                    Write-Host "===== MAVEN ====="
                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" -version
                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven verification failed."
                    }

                    Write-Host "===== DOCKER ====="
                    & docker version
                    if ($LASTEXITCODE -ne 0) {
                        throw "Jenkins cannot access the Docker Engine."
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
                        throw "pom.xml not found in workspace."
                    }

                    & "$env:MAVEN_HOME\\bin\\mvn.cmd" clean test package
                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven build or tests failed."
                    }

                    if (-not (Test-Path "target/student-placement-portal-1.0.0.jar")) {
                        throw "Executable JAR was not generated."
                    }

                    Write-Host "Build and tests passed."
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                    $latestTag = "$env:DOCKER_IMAGE`:latest"

                    if (-not (Test-Path "Dockerfile")) {
                        throw "Dockerfile not found."
                    }

                    Write-Host "Building $imageTag"

                    & docker build -t $imageTag -t $latestTag .
                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image build failed."
                    }

                    Write-Host "Docker image built successfully."
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
                            New-Item -ItemType Directory -Path $dockerConfig -Force |
                                Out-Null

                            $env:DOCKER_CONFIG = $dockerConfig

                            Write-Host "Logging in to Docker Hub..."

                            $env:DH_TOKEN | & docker login `
                                --username "$env:DH_USER" `
                                --password-stdin

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub login failed."
                            }

                            $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                            $latestTag = "$env:DOCKER_IMAGE`:latest"

                            Write-Host "Pushing versioned image..."
                            & docker push $imageTag
                            if ($LASTEXITCODE -ne 0) {
                                throw "Versioned image push failed."
                            }

                            Write-Host "Pushing latest tag..."
                            & docker push $latestTag
                            if ($LASTEXITCODE -ne 0) {
                                throw "Latest image push failed."
                            }

                            Write-Host "Both Docker image tags were pushed."
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

                        Write-Host "===== KUBERNETES CONTEXT ====="
                        & kubectl config current-context
                        if ($LASTEXITCODE -ne 0) {
                            throw "Cannot read Kubernetes context."
                        }

                        $context = (& kubectl config current-context).Trim()
                        if ($LASTEXITCODE -ne 0 -or $context -ne "docker-desktop") {
                            throw "Expected docker-desktop context; found '$context'."
                        }

                        & kubectl cluster-info
                        if ($LASTEXITCODE -ne 0) {
                            throw "Kubernetes cluster is not reachable from Jenkins."
                        }

                        if (-not (Test-Path "deployment.yaml")) {
                            throw "deployment.yaml not found."
                        }

                        $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                        $manifestPath = Join-Path $env:WORKSPACE "deployment-ci.yaml"

                        $manifest = Get-Content "deployment.yaml" -Raw
                        $manifest = $manifest.Replace(
                            "jaisshaurya/student-placement-portal:latest",
                            $imageTag
                        )

                        if (-not $manifest.Contains($imageTag)) {
                            throw "Could not set the manifest to the build image tag."
                        }

                        Set-Content -Path $manifestPath -Value $manifest -Encoding ascii

                        Write-Host "Deploying image $imageTag"
                        & kubectl apply -f $manifestPath
                        if ($LASTEXITCODE -ne 0) {
                            throw "Kubernetes manifest apply failed."
                        }

                        & kubectl rollout status `
                            "deployment/$env:K8S_DEPLOYMENT" `
                            --timeout=180s

                        if ($LASTEXITCODE -ne 0) {
                            throw "Deployment rollout failed."
                        }

                        Write-Host "===== DEPLOYMENT ====="
                        & kubectl get deployment $env:K8S_DEPLOYMENT
                        if ($LASTEXITCODE -ne 0) {
                            throw "Could not inspect deployment."
                        }

                        & kubectl get pods `
                            -l "app=$env:K8S_DEPLOYMENT" `
                            -o wide

                        if ($LASTEXITCODE -ne 0) {
                            throw "Could not inspect application pods."
                        }

                        & kubectl get service $env:K8S_SERVICE
                        if ($LASTEXITCODE -ne 0) {
                            throw "Could not inspect application service."
                        }

                        Remove-Item $manifestPath -Force `
                            -ErrorAction SilentlyContinue
                    '''
                }
            }
        }

        stage('Verify Application') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $url = "http://localhost:30085/api/health"
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
                            Write-Host "Health check attempt $attempt failed; retrying."
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
            echo 'CI/CD pipeline completed: build, tests, Docker Hub push, Kubernetes deployment and health check.'
        }

        failure {
            echo 'CI/CD pipeline failed. Review the first failing stage in Console Output.'
        }
    }
}
'@ | Set-Content -Encoding ascii Jenkinsfile