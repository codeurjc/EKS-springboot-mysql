worker = "worker-${UUID.randomUUID().toString()}"
def tag = "dev"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'maven', image: '303679435142.dkr.ecr.eu-west-1.amazonaws.com/maven-dev:latest', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'e2e', image: '303679435142.dkr.ecr.eu-west-1.amazonaws.com/springboot-e2e:1.0.0', ttyEnabled: true),
  containerTemplate(name: 'kubectl', image: 'nordri/kubectl-helm:v1', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'tools', image: 'nordri/nordri-dev-tools', command: 'cat', ttyEnabled: true),
  containerTemplate(name: 'db', image: 'mysql:5.7', ttyEnabled: true, envVars: [
    envVar(key: 'MYSQL_ROOT_PASSWORD', value: 'r00t'),
    envVar(key: 'MYSQL_DATABASE', value: 'springboot_mysql_example')],
    ports: [portMapping(name: 'mysql', containerPort: 3306, hostPort: 3306)
    ]),
  containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true, envVars: [
    envVar(key: 'TAG', value: tag)
    ])
  ],
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
  ]) {
  
  node(worker) {
    stage('Cloning the repo...') {
      git 'https://github.com/twotoforty/spring-boot-example.git'
    }

    stage('Building...') {
      container('maven') {
        sh """
          mvn --batch-mode -DskipTests=true package
        """
      }
    }

    stage('Integration testing...') {
      container('maven') {
        sh """
          mvn --batch-mode -Dtest=SpringBootMySqlApplicationTests -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/springboot_mysql_example test
        """
      }
    }

    def gitCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
    stage('Building the image...') {
      container('docker'){
        sh """
          cp -v target/*jar docker
          cd docker
          docker build -t ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG}-${gitCommit} .
          docker tag ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG}-${gitCommit} \
                     ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:latest
          docker tag ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG}-${gitCommit} \
                     ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:qa-${gitCommit}
        """
      }
    }

    stage('Cleaning dangling images...') {
      container('docker') {
        sh 'docker images --quiet --filter=dangling=true | xargs --no-run-if-empty docker rmi -f || true'
      }
    }

    stage('Pushing to registry...') {
      container('docker') {
        docker.withRegistry('https://303679435142.dkr.ecr.eu-west-1.amazonaws.com', 'ecr:eu-west-1:d9245da2-b705-4a6a-a99a-bafbc95a36ca') {
          sh """
            docker push ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:\${TAG}-${gitCommit}
            docker push ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:latest
            docker push ${AMAZON_ECS_REGISTRY}/${DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE}:qa-${gitCommit}
          """
        }
      }
    }

    stage('Updating the Kubernetes template...') {
      sh """
        cd helm/springboot-example
        sed "s#TAG_NAME#dev-"${gitCommit}"#" values.yaml > ../values-helm.yaml
        sed -i "s#ENABLE_PERSISTENCE#false#" charts/mysql/values.yaml
        sed -i "s#SERVICE_TYPE#ClusterIP#" ../values-helm.yaml
      """
    }

    stage('Deploying app...') {
      container('kubectl') {
        withKubeConfig([credentialsId: 'a282a0e9-1738-44d0-a3c9-354b132dbc52',
          serverUrl: 'https://kubernetes.default'
        ]) {
          sh 'cd helm && helm install --name springboot-"${BUILD_NUMBER}" --values values-helm.yaml ./springboot-example'
        }
      }
    }

    stage('Waiting for the app to be ready...') {
      container('tools') {
        sh 'cd tools && ./run.sh http://springboot-"${BUILD_NUMBER}"-springboot-example:8080'
      }
    }
    
    stage('Testing...') {
      container('e2e') {
        sh """
          export SUT_URL=http://springboot-"${BUILD_NUMBER}"-springboot-example:8080
          mvn --batch-mode test -Dtest=WebBrowserTest
        """
      }
    }

    stage('Cleaning the house...') {
      container('kubectl') {
        withKubeConfig([credentialsId: 'a282a0e9-1738-44d0-a3c9-354b132dbc52',
          serverUrl: 'https://kubernetes.default'
        ]) {
          sh 'helm delete --purge springboot-"${BUILD_NUMBER}"'
        }
      }
    }

    stage('Updating template for QA environment...') {
      container('tools') {
        sh """
          cd helm/springboot-example
          sed "s#TAG_NAME#qa-"${gitCommit}"#" values.yaml > ../values-helm.yaml
          sed -i "s#ENABLE_PERSISTENCE#false#" charts/mysql/values.yaml
          sed -i "s#SERVICE_TYPE#LoadBalancer#" ../values-helm.yaml
        """
      }
    }

    stage('Updating QA environment...') {
      container('kubectl') {
        withKubeConfig([credentialsId: 'a282a0e9-1738-44d0-a3c9-354b132dbc52',
          serverUrl: 'https://kubernetes.default'
        ]) {
          sh """
            RELEASES=\$(helm list | grep springboot-qaa | wc -l)
            if [ "\${RELEASES}" == "0" ]; then
              cd helm && helm install --name springboot-qa --values values-helm.yaml ./springboot-example
            else
              cd helm && helm upgrade springboot-qa ./springboot-example
            fi
          """
        }
      }
    }
  }
}