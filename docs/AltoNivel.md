# Descripción del Problema

Tenemos una aplicación SpringBoot + MySQL. Queremos tener una aplicación en producción y otra en QA.

## Requisitos

1. Test unitarios.
2. Test end to end.
3. Entorno de QA.
4. Entorno de producción.
5. Poder pasar a producción rápidamente.
6. Poder cambiar a cuaquier versión rápidamente.

## Estructura del Repositorio

- docker: Código para construir las imágenes Docker.
- helm: Plantillas para el gestor de paquetes de Kubernetes Helm.
- k8s: Manifiestos para Kubernetes.
- src: Código fuente.
- tools: Scripts útiles para el proceso de desarrollo.

## Docker

Construimos dos imágenes Docker. Una con la aplicación en sí y otra con las herramientas para ejecutar los tests end to end (e2e).

Contamos con un Docker Registry privado en AWS para subir las imágenes Docker. 

## Helm

En Helm hemos creado los Charts para desplegar la aplicación. La plantilla está parametrizada de forma que podamos elegir qué versión de la aplicación desplegar.

## K8s

Hemos escrito dos manifiestos para el entorno de test/qa y otro para producción. La diferencia radica en que el entorno de producción configura persistencia para la base de datos.

## src

Como decimos este directorio contiene el código fuente del proyecto Maven.

## Tools

Scripts utiles, de momento uno que hace loop hasta que la aplicación está lista.