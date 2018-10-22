#!/usr/bin/env bash

kubectl delete services,pods,deployment -l appName=bootstrap-demo-kubernetes-dns-java
kubectl delete services,pods,deployment bootstrap-demo-kubernetes-dns-java
