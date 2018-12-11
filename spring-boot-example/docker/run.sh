#!/bin/bash -x
set -eu -o pipefail

DB=${DATABASE:-127.0.0.1}

java -jar -Dspring.datasource.url=jdbc:mysql://${DB}:3306/springboot_mysql_example /usr/app/app.jar
