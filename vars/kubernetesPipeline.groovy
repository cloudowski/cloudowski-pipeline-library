#!/usr/bin/env groovy

def buildContainer(String codeRepo, String imageName) {
  stage('Create Container image') {
    container('docker') {
      def myRepo = git url: codeRepo
      def gitCommit = myRepo.GIT_COMMIT
      def gitBranch = myRepo.GIT_BRANCH
      def version = "${gitCommit[0..10]}"

      withCredentials([[$class: 'UsernamePasswordMultiBinding',
        credentialsId: 'dockerhub',
        usernameVariable: 'DOCKER_HUB_USER',
        passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
        sh """
          docker build -t ${imageName}:${version} .
          docker tag ${imageName}:${version} ${imageName}:latest
          #docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}
          #docker push ${imageName}:${version}
          """
      }
    }
  }
}

def initHelm() {
  stage('Run helm') {
    container('helm') {
      helmutils.init()
      helmutils.enableIncubatorRepo()
    }
  }
}

def buildHelm(appName) {
  stage('Build helm package') {
    container('helm') {
      helmutils.init()
      helmutils.build(appName)
      stash name: 'chart', includes: '*.tgz'
      archiveArtifacts artifacts: '*.tgz'
    }
  }
}

def publishChart(appName, repoUrl) {
  stage('Publish helm package') {
    container('helm') {
      helmutils.init()
      helmutils.addRepo('myrepo', repoUrl)
      helmutils.publishChart(appName, 'myrepo')
    }
  }
}

def deployChart(appName, namespace = '',releaseName = '') {
  stage('Deploy helm package') {
    container('kubectl') {
      sh """
        [ '${namespace}x' = 'x' ] && return
        if ! kubectl get ns $namespace 2> /dev/null;then
          echo "Creating namespace $namespace"
          kubectl create ns $namespace
        fi
      """
    }

    container('helm') {
      helmutils.init()
      helmutils.deployChart(appName, namespace, releaseName)
    }
  }
}
