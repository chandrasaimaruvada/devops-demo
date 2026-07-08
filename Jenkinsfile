pipeline {
    agent any

    environment {
        DOCKERHUB_USER   = 'yourdockerhubusername'          // <-- change me
        IMAGE_NAME       = "${DOCKERHUB_USER}/devops-demo"
        IMAGE_TAG        = "${env.BUILD_NUMBER}"
        DEPLOY_HOST      = 'ec2-user@YOUR.EC2.PUBLIC.IP'     // <-- change me
        CONTAINER_NAME   = 'devops-demo-app'
        APP_PORT         = '8081'
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
                sh 'mvn -B clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/devops-demo.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                   usernameVariable: 'DH_USER',
                                                   passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(credentials: ['ec2-ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} "
                            docker pull ${IMAGE_NAME}:latest &&
                            docker stop ${CONTAINER_NAME} || true &&
                            docker rm ${CONTAINER_NAME} || true &&
                            docker run -d --name ${CONTAINER_NAME} -p ${APP_PORT}:${APP_PORT} ${IMAGE_NAME}:latest
                        "
                    '''
                }
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    sleep 10
                    curl -f http://YOUR.EC2.PUBLIC.IP:${APP_PORT}/version
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded — build ${env.BUILD_NUMBER} deployed."
        }
        failure {
            echo "Pipeline failed — check the stage logs above."
        }
    }
}
