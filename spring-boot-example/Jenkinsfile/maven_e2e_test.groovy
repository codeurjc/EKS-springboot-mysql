worker = "worker-${UUID.randomUUID().toString()}"

podTemplate(label: worker, containers: [
  containerTemplate(name: 'e2e', image: '303679435142.dkr.ecr.eu-west-1.amazonaws.com/springboot-e2e:1.0.0', ttyEnabled: true)
]) {
	node(worker) {
		stage('Cloning the repo...') {
			git 'https://github.com/twotoforty/spring-boot-example.git'
		}

		stage('Testing...') {
			container('e2e') {
				sh """
				  export SUT_URL=http://spring-boot-mysql-qa:8080
				  mvn --batch-mode test -Dtest=WebBrowserTest
				"""
			}
		}
	}
}
