# Desplegando Jenkins

Vamos a desplegar Jenkins usando un Chart de Helm. Para ello, previamente necesitamos haber desplegado Helm.

## Persistencia

Vamos a crear un fichero de persistencia (Persistent Volume) para que la cofiguración de Jenkins sea persistente y no se pierda.

Siga los pasos que se muestran aquí

https://docs.aws.amazon.com/es_es/AWSEC2/latest/UserGuide/ebs-creating-volume.html

Recupere el VolumeID que acaba de crear y sustituyalo en la plantilla siguiente:

```
apiVersion: v1
kind: PersistentVolume
metadata:
  name: jenkins-pv
  labels:
    type: jenkins-volume
spec:
  capacity:
    storage: TAMAÑO en GB 
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  awsElasticBlockStore:
    volumeID: ID del Volumen de EBS
    fsType: ext4
```

a continuación configure el Claim sobre el storage

```
---
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: jenkins-pvc
  labels:
    type: jenkins-volume
spec:
  accessModes:
    - ReadWriteOnce
  volumeMode: Filesystem
  resources:
    requests:
      storage: TAMAÑO en GB
```

Por último aplique los cambios al cluster:

`$ kubectl create -f jenkins-pv.yaml -f jenkins-pvc.yaml`

Ahora el volumen de EBS que hemos creado queda vinculada a la instancia de EC2 donde está Jenkins. También lo hemos configurado para que retenga la información, es decir, en caso de destre y que tengamos que desplegar de nuevo Jenkins, no borrará la información que se encuentra en el volumen.

## Fichero jenkins-values.yaml

En este fichero es donde vamos a configurar Jenkins para el despliegue. Se configura:

* AdminPassword: Contraseña del administrador
* JavaOpts: Opciones que se le pasan a la máquina virtual de Java. Por ejemplo los límites de memoría o el _timezone_
* Plugins:
	- cloudbees-folder
    - command-launcher
    - config-file-provider
    - kubernetes-cli
    - kubernetes-credentials
    - mailer
    - ssh-agent
    - amazon-ecr
    - github
    - copyartifact 
* Persistencia: Añadimos la persistencia que hemos creado en el apartado anterior.
* rbac: la ponemos a true para que Jenkins puede manejar la API de Kubernetes.

## Instalando Jenkins

Una vez configurado el fichero de valores, podemos desplegar Jenkins:

`$ helm install --name jenkins -f jenkins-values.yaml stable/jenkins`

Si estamos redesplegando hay que ajustar el valor de _Jenkins URL_ en la configuración.

Cuando este listo, podremos acceder usando la URL del balanceador que se ha creado.
