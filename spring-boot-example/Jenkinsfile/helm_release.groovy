worker = "worker-${UUID.randomUUID().toString()}"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'kubectl', image: 'nordri/kubectl-helm:v1', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true, envVars: [
    envVar(key: 'TAG', value: "${TAG}"),
    envVar(key: 'RELEASE', value: "${VERSION}")
    ]),
  ], 
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
  ]) {

  node(worker) {

    stage('Cloning the repo...') {
        git 'https://github.com/twotoforty/spring-boot-example.git'
    }

    stage('Tagging the image...') {
      container('docker'){
        sh """
          docker tag ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG} \
          ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${RELEASE}
        """
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
        cd helm/springboot-example
        sed "s#TAG_NAME#\"${VERSION}\"#" values.yaml > ../values-helm.yaml
        sed -i "s#ENABLE_PERSISTENCE#true#" charts/mysql/values.yaml
        sed -i "s#SERVICE_TYPE#LoadBalancer#" ../values-helm.yaml
      """
    }

    stage('login into k8s...') {
      container('kubectl') {
        withKubeConfig([credentialsId: 'a282a0e9-1738-44d0-a3c9-354b132dbc52',
          serverUrl: 'https://kubernetes.default'
        ]) {
        sh """
            RELEASES=\$(helm list | grep springboot-prod | wc -l)
            if [ "\${RELEASES}" == "0" ]; then
              cd helm && helm install --name springboot-prod --values values-helm.yaml ./springboot-example
            else
              cd helm && helm upgrade springboot-prod ./springboot-example
            fi
          """
        }
      }
    }
  }
}