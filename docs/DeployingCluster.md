# Despliegue del cluster

Vamos a desplegar el cluster Kubernetes usando Amazon Elastic Container Service for Kubernetes (EKS). Para ello necesitamos previamente una cuenta de AWS.

Vamos a usar Amazon CLI para ir provisionando el Cluster, para instalarlo seguir la documentación:

https://docs.aws.amazon.com/es_es/cli/latest/userguide/installing.html

## Datos de entrada

Nombre del cluster: Fortypersona

## Creación del rol

Necesitamos un rol de AWS con capacidad para crear recursos.

```
cat >policy<<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "autoscaling:DescribeAutoScalingGroups",
                "autoscaling:UpdateAutoScalingGroup",
                "ec2:AttachVolume",
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:CreateRoute",
                "ec2:CreateSecurityGroup",
                "ec2:CreateTags",
                "ec2:CreateVolume",
                "ec2:DeleteRoute",
                "ec2:DeleteSecurityGroup",
                "ec2:DeleteVolume",
                "ec2:DescribeInstances",
                "ec2:DescribeRouteTables",
                "ec2:DescribeSecurityGroups",
                "ec2:DescribeSubnets",
                "ec2:DescribeVolumes",
                "ec2:DescribeVolumesModifications",
                "ec2:DescribeVpcs",
                "ec2:DetachVolume",
                "ec2:ModifyInstanceAttribute",
                "ec2:ModifyVolume",
                "ec2:RevokeSecurityGroupIngress",
                "elasticloadbalancing:AddTags",
                "elasticloadbalancing:ApplySecurityGroupsToLoadBalancer",
                "elasticloadbalancing:AttachLoadBalancerToSubnets",
                "elasticloadbalancing:ConfigureHealthCheck",
                "elasticloadbalancing:CreateListener",
                "elasticloadbalancing:CreateLoadBalancer",
                "elasticloadbalancing:CreateLoadBalancerListeners",
                "elasticloadbalancing:CreateLoadBalancerPolicy",
                "elasticloadbalancing:CreateTargetGroup",
                "elasticloadbalancing:DeleteListener",
                "elasticloadbalancing:DeleteLoadBalancer",
                "elasticloadbalancing:DeleteLoadBalancerListeners",
                "elasticloadbalancing:DeleteTargetGroup",
                "elasticloadbalancing:DeregisterInstancesFromLoadBalancer",
                "elasticloadbalancing:DeregisterTargets",
                "elasticloadbalancing:DescribeListeners",
                "elasticloadbalancing:DescribeLoadBalancerAttributes",
                "elasticloadbalancing:DescribeLoadBalancerPolicies",
                "elasticloadbalancing:DescribeLoadBalancers",
                "elasticloadbalancing:DescribeTargetGroupAttributes",
                "elasticloadbalancing:DescribeTargetGroups",
                "elasticloadbalancing:DescribeTargetHealth",
                "elasticloadbalancing:DetachLoadBalancerFromSubnets",
                "elasticloadbalancing:ModifyListener",
                "elasticloadbalancing:ModifyLoadBalancerAttributes",
                "elasticloadbalancing:ModifyTargetGroup",
                "elasticloadbalancing:ModifyTargetGroupAttributes",
                "elasticloadbalancing:RegisterInstancesWithLoadBalancer",
                "elasticloadbalancing:RegisterTargets",
                "elasticloadbalancing:SetLoadBalancerPoliciesForBackendServer",
                "elasticloadbalancing:SetLoadBalancerPoliciesOfListener",
                "kms:DescribeKey"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": "iam:CreateServiceLinkedRole",
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "iam:AWSServiceName": "elasticloadbalancing.amazonaws.com"
                }
            }
        }
    ]
}
EOF
```

`$ aws iam create-role --role-name KubernetesRole --assume-role-policy-document file:///PATH/TO/policy.json`

## Crear la VPC

Vamos a configurar un Virtual Private Cloud para Kubernetes. Esto no dara más aislamiento en los contenedores.

```
$ cat >parameters.json<<EOF
[
  {"ParameterKey":"VpcBlock","ParameterValue":"192.168.0.0/16"},
  {"ParameterKey":"Subnet01Block","ParameterValue":"192.168.64.0/18"},
  {"ParameterKey":"Subnet02Block","ParameterValue":"192.168.128.0/18}"},
  {"ParameterKey":"Subnet03Block","ParameterValue":"192.168.192.0/18"}
]
EOF
```

```
$ aws cloudformation create-stack \
  --stack-name Fortypersona \
  --template-url https://amazon-eks.s3-us-west-2.amazonaws.com/cloudformation/2018-11-07/amazon-eks-vpc-sample.yaml \
  --parameters file:///PATH/TO/parameters.json \
  --disable-rollback
```

## Crear el Master del cluster

Ahora que tenemos la red y el rol configurados podemos crear el master del clúster, es la máquina que controlará el cluster, sustituir los valores por los adecuados en cada caso.

```
$ aws eks create-cluster \
  --name Fortypersona \
  --role-arn arn:aws:iam::303679435142:role/KubernetesRole \
  --resources-vpc-config subnetIds=subnet-08415098923def71a,subnet-0d5fc19ee2f96d6f1,subnet-0e5a5155f2406e49d
```

## Conectar nuestro _kubectl_ al cluster

Para poder autenticar contra el cluster tenemos que seguir estos pasos:

https://docs.aws.amazon.com/eks/latest/userguide/getting-started.html#eks-configure-kubectl

Una vez hemos resuelto las dependencias podemos ejecutar:

`$ aws eks update-kubeconfig --name Fortypersona`

## Crear los nodos del cluster

Los nodos del cluster se crean usando una plantilla de CloudFormation que configura un grupo de escalabilidad. Es decir, nos facilita la creación de un cluster dinámico que puede escalar aunque de momento solo escala manualmente.

Creamos el fichero con los parámetros:

```
$ cat >parameters.json <<EOF
[
  {"ParameterKey":"BootstrapArguments","ParameterValue":""},
  {"ParameterKey":"ClusterControlPlaneSecurityGroup","ParameterValue":"ID Del grupo de seguridad por defecto de la VPC"},
  {"ParameterKey":"ClusterName","ParameterValue":"Fortypersona"},
  {"ParameterKey":"KeyName","ParameterValue":"RSA Key para acceder a los nodos"},
  {"ParameterKey":"NodeAutoScalingGroupMaxSize","ParameterValue":"1"},
  {"ParameterKey":"NodeAutoScalingGroupMinSize","ParameterValue":"1"},
  {"ParameterKey":"NodeGroupName","ParameterValue":"FortyWorkers"},
  {"ParameterKey":"NodeImageId","ParameterValue":"ami-00c3b2d35bddd4f5c"},
  {"ParameterKey":"NodeInstanceType","ParameterValue":"c5.2xlarge"},
  {"ParameterKey":"NodeVolumeSize","ParameterValue":"20"},
  {"ParameterKey":"Subnets","ParameterValue":"subnet-08415098923def71a"},
  {"ParameterKey":"VpcId","ParameterValue":"vpc-0f059d53d0bfdc9ed"}
]
EOF
```

Y creamos el grupo de escalabilidad

```
$ aws cloudformation create-stack \
  --stack-name ${CLUSTER_NAME} \
  --template-url https://amazon-eks.s3-us-west-2.amazonaws.com/cloudformation/2018-11-07/amazon-eks-nodegroup.yaml \
  --parameters file:///home/nordri/Proyectos/2to40/Kubernetes/parameters.json \
  --disable-rollback \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
```

Para permitir que los nodos se unan al cluster de Kubernetes:

Tomamos el varlor de ARN que ha generado el CloudFormation:

`$ ARN_ROLE=$(aws cloudformation describe-stacks --stack-name Fortypersona | jq --raw-output ' .Stacks[] | .Outputs[] | select(.OutputKey | contains("NodeInstanceRole")) | .OutputValue' )`

y generamos un Config Map:

```
$ cat >aws-auth-cm.yaml<<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: aws-auth
  namespace: kube-system
data:
  mapRoles: |
    - rolearn: ${ARN_ROLE}
      username: system:node:{{EC2PrivateDNSName}}
      groups:
        - system:bootstrappers
        - system:nodes
EOF
```

`$ kubectl apply -f aws-auth-cm.yaml`