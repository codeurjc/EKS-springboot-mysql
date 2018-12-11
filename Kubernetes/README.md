# Kubernetes

En este directorio disponemos de los ficheros propios de configuración de Kubernetes.

## Persistent volume

Primero debemos crear un volumen en AWS EBS para almacenar los datos de Jenkins.

https://docs.aws.amazon.com/es_es/AWSEC2/latest/UserGuide/ebs-creating-volume.html

Anote el Volume ID del volumen que acaba de crear para rellenar el manifiesto de Kubernetes. Configure igualmente el tamaño del volumen, por ejemplo

```
...
storage: 50Gi
...
```

Cree el recurso

`$ kubectl create -f jenkins-pv.yaml`

## Persistent volume claim

Configure aquí el mismo tamaño que hemos dado al Persistent Volume

```
...
storage: 50Gi
...
```

y cree el recurso 

`$ kubectl create -f jenkins-pvc.yaml`

## Storage class

Usamos **Storage class** para provisionar de persistencia nuestra aplicación. Cuando configuramos el manifiesto o la plantilla Helm, podemos indicar que use persistencia de esta clase de almacenamiento así de forma que cada vez que solicitemos persistencia, se creará un volumen automáticamente en AWS y se acoplará al contenedor correspondiente.

Hemos llamado a la clase _aws_ebs_. Si cambia el nombre, recuerde actualizar las plantillas que hacen referencia a esta clase.

## Jenkins values

En el fichero _jenkins_values.yaml_ está la configuración para desplegar Jenkins con una plantilla de Helm.