# Akka Cluster Bootstrap using DNS in Kubernetes in Java

Example project showing how DNS to bootstrap an Akka Cluster in Kubernetes.

## Deploying in Minikube

Set the docker environment to use minikube:

`eval $(minikube docker-env)`

Install the docker image locally:

`sbt docker:publishLocal`

Deploy the application:

`./kube-create.sh`

Finally to delete everything

`./kube-delete.sh`

