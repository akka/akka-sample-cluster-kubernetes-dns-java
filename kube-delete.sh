#!/usr/bin/env bash

kubectl delete services,pods,deployment -l appName=akka-sample-cluster-kubernetes-dns-java
kubectl delete services,pods,deployment akka-sample-cluster-kubernetes-dns-java
