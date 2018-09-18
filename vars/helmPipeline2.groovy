#!/usr/bin/env groovy

def call(body) {

  def pipelineParams= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  env.REPO = pipelineParams.appRepo
  env.APP_NAME = pipelineParams.appName

  println "Application repository: ${REPO}"

  withKubernetesTools {

      kubernetesPipeline.buildContainer(REPO, 'cloudowski/fussy')
      kubernetesPipeline.buildHelm(APP_NAME)
      kubernetesPipeline.publishChart(APP_NAME,'http://charts.192.168.99.100.nip.io/')
      kubernetesPipeline.deployChart(APP_NAME, "${APP_NAME}-test", "${APP_NAME}")
      kubernetesPipeline.deployChart(APP_NAME, 'default', "${APP_NAME}-test")
      kubernetesPipeline.deployChart(APP_NAME, 'default', "${APP_NAME}-stage")
      kubernetesPipeline.deployChart(APP_NAME, 'default', "${APP_NAME}-prod")

  }
}
