pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'chandrasai256'
        IMAGE_NAME = "${DOCKERHUB_USER}/devops-demo"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                        docker buildx create --use --name mybuilder || true
                        docker buildx inspect --bootstrap

                        docker buildx build \
                          --platform linux/amd64,linux/arm64 \
                          -t ${IMAGE_NAME}:${IMAGE_TAG} \
                          -t ${IMAGE_NAME}:latest \
                          --push .
                    '''
                }
            }
        }

    }

    post {

        success {
            echo "✅ Build ${BUILD_NUMBER} completed successfully."
        }

        failure {
            echo "❌ Pipeline failed."
        }

        always {
            cleanWs()
        }
    }
}