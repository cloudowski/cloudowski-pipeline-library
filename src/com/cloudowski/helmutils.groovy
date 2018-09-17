#!/usr/bin/env groovy

package com.cloudowski

def init() {
  def returnStatus = sh returnStatus: true, script: """
    if ! helm  version -s 2> /dev/null;then
     echo "Tiller not available - installing"
     helm init
    else
      echo "Helm already initialized"
    fi
    helm init --client-only
    """
  def pluginStatus = installPlugins()
  return (returnStatus == 0) && (pluginStatus == 0)

}

def installPlugins() {
  def returnStatus = sh returnStatus: true, script: """
    if ! [ -d ~/.helm/plugins/push ];then
      echo "Installing push plugin for helm"
      mkdir ~/.helm/plugins/push
      wget -q -O- https://github.com/chartmuseum/helm-push/releases/download/v0.7.1/helm-push_0.7.1_linux_amd64.tar.gz |tar -xpzf- -C ~/.helm/plugins/push
    fi
    """
  return returnStatus == 0
}

def addRepo(repoName, repoUrl) {
  def returnStatus = sh returnStatus: true, script: """
    echo "Adding Helm repo ${repoName} from ${repoUrl}"
    helm repo add ${repoName} ${repoUrl}
    helm repo update ${repoName}
    """
  return returnStatus == 0
}

def enableIncubatorRepo() {
  addRepo('incubator','https://kubernetes-charts-incubator.storage.googleapis.com/')
}

def build(appName) {
  // stupid requirement of helm - dir needs to be named exactly as Chart
  def returnStatus = sh returnStatus: true, script: """
    [ -d ${appName} ] || mkdir ${appName}; cp -r . ${appName}/ || true
    echo "Building Helm package"
    helm package ${appName}
    """

  return returnStatus == 0
}

def publishChart(appName, repo) {
  def returnStatus = sh returnStatus: true, script: """
    [ -d ${appName} ] || mkdir ${appName}; cp -r . ${appName}/ || true

    echo "Publishing helm package"
    helm push ${appName} ${repo}
    """

  return returnStatus == 0
}

def deployChart(appName, namespace, releaseName) {
  def helmOpts = ''
  if (namespace != '') {
    helmOpts += " --namespace ${namespace}"
  }

  if (releaseName != '') {
    helmOpts += " --name ${releaseName}"
  }

  def returnStatus = sh returnStatus: true, script: """

    echo "Deploying chart ${appName}"
    helm install ${helmOpts} ${appName}
    """

  return returnStatus == 0
}

return this
