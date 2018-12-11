worker = "worker-${UUID.randomUUID().toString()}"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true, envVars: [
    envVar(key: 'TAG', value: "${TAG}")
    ])
  ],
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
  ]) {

  node(worker) {

    stage('Cloning the repo...') {
      git 'https://github.com/twotoforty/spring-boot-example.git'
    }

    stage('Building the image...') {
      container('docker'){
        sh """
          cd docker/e2e
          docker build -t ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_E2E_TEST}:\${TAG} .
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
            docker push ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_E2E_TEST}:\${TAG}
          """
        }
      }
    }
  }
}
