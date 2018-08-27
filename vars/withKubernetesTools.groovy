#!/usr/bin/env groovy

import com.cloudowski.*

def call(Closure body) {

  def label = "worker-${UUID.randomUUID().toString()}"
  def podServiceAccount = 'jenkins'

  // nasty things with podTemplate and multiple containers with required tools..
  podTemplate(label: label, serviceAccount: podServiceAccount, containers: [
    containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.10.7', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v2.10.0', command: 'cat', ttyEnabled: true)
  ],
  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
  ]) {
    node(label) {
      body()
    }
  }
}
