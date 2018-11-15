#!/bin/bash

set -exu

sbt docker:publishLocal

kubectl apply -f kubernetes/akka-cluster.yml

for i in {1..10}
do
  echo "Waiting for pods to get ready..."
  kubectl get pods
  [ `kubectl get pods | grep Running | wc -l` -eq 3 ] && break
  sleep 4
done

if [ $i -eq 10 ]
then
  echo "Pods did not get ready"
  exit -1
fi

POD=$(kubectl get pods | grep akka-sample-cluster-kubernetes-dns-java | grep Running | head -n1 | awk '{ print $1 }')

for i in {1..10}
do
  echo "Checking for MemberUp logging..."
  kubectl logs $POD akka-sample-cluster-kubernetes-dns-java | grep MemberUp || true
  [ `kubectl logs $POD akka-sample-cluster-kubernetes-dns-java | grep MemberUp | wc -l` -eq 3 ] && break
  sleep 3
done

if [ $i -eq 10 ]
then
  echo "No 3 MemberUp log events found"
  for POD in $(kubectl get pods | grep akka-sample-cluster-kubernetes-dns-java | grep Running | awk '{ print $1 }')
  do
    echo "Logging for $POD"
    kubectl logs $POD akka-sample-cluster-kubernetes-dns-java
  done
  exit -1
fi
