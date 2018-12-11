worker = "worker-${UUID.randomUUID().toString()}"
def tag = "dev"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.10.9', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true, envVars: [
    envVar(key: 'TAG', value: tag),
    envVar(key: 'RELEASE', value: "${VERSION}")
    ])
  ],
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
  ]) {

  node(worker) {

    stage('Cloning the repo...') {
      git 'https://github.com/twotoforty/spring-boot-example.git'
    }
    
    def gitCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
    stage('Tagging the image...') {
      container('docker'){
        sh """
          docker tag ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG}-${gitCommit} \
          ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${RELEASE}
        """
      }
    }

    stage('Cleaning dangling images...') {
      container('docker') {
        sh 'docker images --quiet --filter=dangling=true | xargs --no-run-if-empty docker rmi -f'
      }
    }

    stage('Pushing to registry...') {
      container('docker') {
        docker.withRegistry('https://303679435142.dkr.ecr.eu-west-1.amazonaws.com', 'ecr:eu-west-1:d9245da2-b705-4a6a-a99a-bafbc95a36ca') {
          sh """
            docker push ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${RELEASE}
          """
        }
      }
    }

    stage('Updating the Kubernetes template...') {
      sh """
        cd k8s
        sed "s#SPRINGBOOT_EXAMPLE_DOCKER_IMAGE#${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\"${VERSION}\"#" spring-boot-mysql.yml.template > spring-boot-mysql.yml
        cat spring-boot-mysql.yml
      """
    }

    stage('Deploying app...') {
      container('kubectl') {
        withKubeConfig([credentialsId: 'a282a0e9-1738-44d0-a3c9-354b132dbc52',
          serverUrl: 'https://kubernetes.default'
        ]) {
        sh 'cd k8s && kubectl apply -f spring-boot-mysql.yml'
        }
      }
    }
  }
}