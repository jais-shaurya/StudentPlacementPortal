pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        JAVA_HOME      = 'C:\\Program Files\\Eclipse Adoptium\\jdk-25.0.3.9-hotspot'
        MAVEN_HOME     = 'C:\\Users\\Asus\\apache-maven-3.9.16-bin\\apache-maven-3.9.16'
        DOCKER_IMAGE   = 'jaisshaurya/student-placement-portal'
        K8S_DEPLOYMENT = 'student-placement-portal-app'
        K8S_SERVICE    = 'student-placement-portal-app-service'
    }

    stages {

        stage('Verify Environment') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    Write-Host "===== VERIFY JAVA ====="
                    $javaExe = Join-Path $env:JAVA_HOME 'bin\\java.exe'

                    if (-not (Test-Path $javaExe)) {
                        throw "Java executable not found: $javaExe"
                    }

                    Write-Host "Java executable found."

                    Write-Host "===== VERIFY MAVEN ====="
                    $mvn = Join-Path $env:MAVEN_HOME 'bin\\mvn.cmd'

                    if (-not (Test-Path $mvn)) {
                        throw "Maven executable not found: $mvn"
                    }

                    & $mvn -version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven verification failed."
                    }

                    Write-Host "===== VERIFY DOCKER ====="
                    docker version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker verification failed."
                    }

                    Write-Host "Environment verification succeeded."
                '''
            }
        }

        stage('Build and Test') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    Write-Host "===== MAVEN CLEAN TEST PACKAGE ====="

                    $mvn = Join-Path $env:MAVEN_HOME 'bin\\mvn.cmd'
                    & $mvn clean test package

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven build or tests failed."
                    }

                    $jarPath = Join-Path $env:WORKSPACE `
                        'target\\student-placement-portal-1.0.0.jar'

                    if (-not (Test-Path $jarPath)) {
                        throw "Expected JAR was not created: $jarPath"
                    }

                    Write-Host "Build and tests succeeded."
                    Write-Host "JAR: $jarPath"
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                    $latestTag = "$env:DOCKER_IMAGE`:latest"

                    Write-Host "===== BUILD DOCKER IMAGE ====="
                    Write-Host "Versioned image: $imageTag"

                    docker build -t $imageTag -t $latestTag .

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

                        $authDir = Join-Path $env:WORKSPACE `
                            ".docker-auth-$env:BUILD_NUMBER"

                        $tokenFile = Join-Path $authDir 'stdin.txt'
                        $dockerExe = (Get-Command docker.exe -ErrorAction Stop).Source
                        $cmdExe = Join-Path $env:SystemRoot 'System32\\cmd.exe'

                        try {
                            New-Item -ItemType Directory `
                                -Path $authDir -Force | Out-Null

                            # Write the token as UTF-8 without a BOM.
                            $utf8 = New-Object System.Text.UTF8Encoding($false)

                            [System.IO.File]::WriteAllText(
                                $tokenFile,
                                $env:DH_TOKEN + "`n",
                                $utf8
                            )

                            Write-Host "===== DOCKER HUB LOGIN ====="

                            # Preserve the working Windows stdin-redirection method.
                            $dockerArgs = '--config "' + $authDir +
                                '" login --username "' + $env:DH_USER +
                                '" --password-stdin < "' + $tokenFile + '"'

                            $cmd = '"' + $dockerExe + '" ' + $dockerArgs

                            & $cmdExe /d /s /c ('"' + $cmd + '"')

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub login failed."
                            }

                            Write-Host "DOCKER_CLI_LOGIN_SUCCESS"

                            $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                            $latestTag = "$env:DOCKER_IMAGE`:latest"

                            Write-Host "===== PUSH VERSIONED IMAGE ====="

                            & $dockerExe --config $authDir push $imageTag

                            if ($LASTEXITCODE -ne 0) {
                                throw "Versioned image push failed."
                            }

                            Write-Host "===== PUSH LATEST IMAGE ====="

                            & $dockerExe --config $authDir push $latestTag

                            if ($LASTEXITCODE -ne 0) {
                                throw "Latest image push failed."
                            }

                            Write-Host "Docker Hub publishing succeeded."
                        }
                        finally {
                            if (Test-Path $authDir) {
                                & $dockerExe --config $authDir logout 2>$null |
                                    Out-Null
                            }

                            Remove-Item -Recurse -Force $authDir `
                                -ErrorAction SilentlyContinue
                        }
                    '''
                }
            }
        }

        stage('Deploy Kubernetes') {
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

                        $manifest = Join-Path $env:WORKSPACE 'deployment.yaml'
                        $tempManifest = Join-Path $env:WORKSPACE 'deployment-ci.yaml'

                        try {
                            if (-not (Test-Path $manifest)) {
                                throw "Kubernetes manifest not found: $manifest"
                            }

                            Write-Host "===== KUBERNETES CONTEXT ====="

                            kubectl config current-context

                            if ($LASTEXITCODE -ne 0) {
                                throw "Unable to read Kubernetes context."
                            }

                            $context = (kubectl config current-context).Trim()

                            if ($context -ne 'docker-desktop') {
                                throw "Unexpected Kubernetes context: $context"
                            }

                            kubectl cluster-info

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes cluster is unavailable."
                            }

                            Write-Host "===== PREPARE DEPLOYMENT MANIFEST ====="

                            $imageTag = "$env:DOCKER_IMAGE`:$env:BUILD_NUMBER"
                            $yaml = Get-Content -Path $manifest -Raw

                            $imagePattern = '(?m)^([ \t]*image:[ \t]*)jaisshaurya/student-placement-portal:\S+[ \t]*$'

                            if ($yaml -notmatch $imagePattern) {
                                throw "Expected application image entry was not found in deployment.yaml."
                            }

                            $yaml = [regex]::Replace(
                                $yaml,
                                $imagePattern,
                                ('$1' + $imageTag)
                            )

                            Set-Content -Path $tempManifest `
                                -Value $yaml -Encoding UTF8

                            Write-Host "Deploying image: $imageTag"

                            Write-Host "===== APPLY KUBERNETES MANIFEST ====="

                            kubectl apply -f $tempManifest

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes manifest application failed."
                            }

                            Write-Host "===== WAIT FOR ROLLOUT ====="

                            kubectl rollout status `
                                "deployment/$env:K8S_DEPLOYMENT" `
                                --timeout=180s

                            if ($LASTEXITCODE -ne 0) {
                                throw "Kubernetes deployment rollout failed."
                            }

                            Write-Host "===== DEPLOYMENT STATUS ====="
                            kubectl get deployment $env:K8S_DEPLOYMENT

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not retrieve deployment status."
                            }

                            kubectl get pods -o wide

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not retrieve pod status."
                            }

                            kubectl get service $env:K8S_SERVICE

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not retrieve service status."
                            }

                            Write-Host "Kubernetes deployment succeeded."
                        }
                        finally {
                            Remove-Item $tempManifest `
                                -Force -ErrorAction SilentlyContinue

                            Remove-Item Env:\\KUBECONFIG `
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

                    $healthUrl = 'http://localhost:30085/api/health'
                    $maxAttempts = 12
                    $healthy = $false

                    Write-Host "===== APPLICATION HEALTH CHECK ====="

                    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
                        try {
                            $response = Invoke-WebRequest `
                                -Uri $healthUrl `
                                -UseBasicParsing `
                                -TimeoutSec 10

                            if ($response.StatusCode -eq 200) {
                                Write-Host "Response: $($response.Content)"
                                Write-Host "Application health check passed."
                                $healthy = $true
                                break
                            }
                        }
                        catch {
                            Write-Host "Health check attempt $attempt failed."
                        }

                        if ($attempt -lt $maxAttempts) {
                            Start-Sleep -Seconds 5
                        }
                    }

                    if (-not $healthy) {
                        throw "Application health check failed after $maxAttempts attempts."
                    }
                '''
            }
        }
    }

    post {
        always {
            junit(
                testResults: '**/target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'target/student-placement-portal-1.0.0.jar',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        success {
            echo 'CI/CD succeeded: SCM checkout, tests, Docker publishing, Kubernetes deployment and health verification.'
        }

        failure {
            echo 'CI/CD failed. Check the stage logs above to identify the failure.'
        }
    }
}