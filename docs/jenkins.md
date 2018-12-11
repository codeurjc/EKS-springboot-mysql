# Jenkins

## Credenciales

Debemos almacenar tres credenciales:

1. Las credenciales de AWS para acceder al registry privado.
2. Un token de GitHub para registrar los hooks en el repositorio.
3. Una cuenta para kubernetes.

## Configuración general

Tenemos que configurar un servidor de Git donde registrar los hooks del repositorio. Usaremos el token que hemos configurado en credenciales.

Hemos configurado variables de entorno para que sea más transversal hacer cambios de configuración, son las siguientes:

| Variable | Valor | Descripción |
| -------- | ----- | ----------- |
| AMAZON_ECS_REGISTRY | 303679435142.dkr.ecr.eu-west-1.amazonaws.com | Registry Docker privado |
| DOCKER_IMAGE_NAME_E2E_TEST | springboot-e2e | Nombre de la imagen Docker para pruebas e2e |
| DOCKER_IMAGE_NAME_SPRING_BOOT_EXAMPLE | springboot-example | Nombre de la imagen Docker para la aplicación |

## Jobs

Hemos creado Pipelines para resolver las tareas del ciclo de integración continua.

Pero en primer lugar describimos el método de trabajo, al tratarse de Kubernetes lo que creamos es un pod con los contiene los contenedores que vamos a usar durante el pipeline. También definimos las variables de entorno y los volumenes que mapeamos dentro de los contenedores.

Igualmente, al tratarse de Kubernetes, no tenemos workers al estilo tradicional, por lo que generamos uno en cada ejecución del pipeline y lo nombramos con un nombre aleatorio para evitar colisiones.

* Big_pipeline_test

La definición del pipeline se encuentra en `jenkinsfile_pre.groovy`

En este pipeline hacemos la integración continua. Vamos a ir describiendo cada stage.

Los pasos a seguir:

1. Clonar el repo.
2. Construir el paquete.
3. Ejecutar test unitarios.
4. Construir la imagen Docker de desarrollo que se etiquetará con el commit id del último push, la etiqueta de qa y con la etiqueta de latest.
5. Limpiamos las imagenes de Docker de tipo _dangling_ que son imagenes sin etiqueta y que acaban ocupando mucho espacio en disco.
6. Se suben las tres imagenes Docker al registry privado.
7. Se actualiza el manifest de Kubernetes con la imagen docker de desarrollo que hemos creado.
8. Se despliega la aplicación de dev usando el manifest de kubernetes.
9. Esperamos que la aplicación esté arriba y corriendo.
10. Lanzamos el test e2e.
11. Si hasta aquí está todo bien, eliminamos la aplicación que hemos desplegado en desarrollo.
12. Actualizamos el manifest de Kubernetes con el tag de QA.
13. Desplegamos el entorno de QA. Este entorno se quedara desplegado en la infraestructura.

* Big_pipeline_release

La definición del pipeline se encuentra en `jenkinsfile_release.groovy`

La release se hace a mano estableciendo el valor del tag de release y conociendo el tag de commit id que va a ser promocionado a release.

En esta ocasión los pasos son:

1. Clonar el repo.
2. Tagear la imagen de dev como release.
3. Limpiar las imagenes docker dangling.
4. Empujar la imagen al repositorio.
5. Actualizar el manifiesto de Kubernetes.
6. Desplegar la aplicación.

* Build_e2e_container

La definición del pipeline se encuentra en `jenkinsfile_build_e2e_docker_image.groovy`

Este job construye una imagen Docker para testar nuestra aplicación. Este job se ejecuta manualmente, por lo que el tag de la imagen Docker se necesita como parámetro de entrada.

La imagen contiene, entre otros, un navegador, Maven y Java

Los pasos de este job son sencillos,

1. Clonar el repo.
2. Construir la imagen.
3. Limpiar las imagenes docker dangling.
4. Empujar al repositorio.

* change_release

La definición del pipeline se encuentra en `jenkinsfile_set_release.groovy`

Este job sirve en el supuesto de encontrar un problema con la release que está desplegada y queremos cambiar la versión. Básicamente es un estracto de los jobs de release y test en los que etiquetamos el manifiesto con la versión que queremos desplegar en estos momentos.

Así los pasos son:

1. Clonar el repo.
2. Actualizar el tag del manifiesto.
3. Actualizar el despliegue.

* Helm_Pre

Hemos escrito un chart para trabajar con Helm. El Chart depende de MySQL por lo que la plantilla para la base de datos se incluye dentro del directorio charts.

En el pipeline nos hemos basado en el anterior de _pre_ y hemos sustituido los stages donde usamos Kubernetes por los que usamos Helm. También hemos usando un nuevo contenedor que incluye tanto _kubectl_ como _helm_.

En el Chart hemos dejado parametrizado el tag de la imagen docker que vamos a usar, la persistencia (no en dev y qa, si en prod) y el Service Type, lo dejamos como cluster IP para dev y en LoadBalancer para qa y prod.

El desplegar con Helm tiene la peculiaridad de que se hace de forma distinta si ya existe un deployment previo por lo que hemos escrito la casuistica y comprobamos si existe o no para actuar en consecuencia.

* Helm_Release

Si queremos hacer la release con Helm también nos hemos basado en el pipeline con Kubernetes.

Hemos añadido el contenedor con _kubectl_ y _helm_ para poder operar contra el cluster Kubernetes.

También se taggea la imagen que queremos promocionar a producción y se sube a desarrollo.

**Nota**: Para que funcione debe existir un despliegue previo de la aplicación.