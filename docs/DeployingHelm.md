# Helm - Gestor de paquetes

## Instalar Helm

- Descargar:

https://github.com/kubernetes/helm/releases

- Descomprimir

`$ tar -zxvf helm-$VERSION-linux-amd64.tgz`

- Colocar el binario en su sitio

`$ sudo mv linux-amd64/helm /usr/local/bin`

## Autorización

Ejecutar esta línea para otorgar autorización a Helm

`$ kubectl create -f https://raw.githubusercontent.com/nordri/kubernetes-experiments/master/Pipeline/ServiceAccount.yaml`

## Iniciar 

`$ helm init --service-account tiller `

- Comprobar

`$ kubectl --namespace=kube-system get pods --selector app=helm`

```
NAME                            READY     STATUS    RESTARTS   AGE
tiller-deploy-78f96d6f9-8kc88   1/1       Running   0          2m
```

