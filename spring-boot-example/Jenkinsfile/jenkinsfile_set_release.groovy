worker = "worker-${UUID.randomUUID().toString()}"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.10.9', command: 'cat', ttyEnabled: true)
  ]) {

  node(worker) {

    stage('Cloning the repo...') {
        git 'https://github.com/twotoforty/spring-boot-example.git'
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