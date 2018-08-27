#!/usr/bin/env groovy

import com.cloudowski.*

def call(body) {

  def pipelineParams= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  env.REPO = pipelineParams.appRepo
  env.APP_NAME = pipelineParams.appName

  println "Application repository: ${REPO}"

  withKubernetesTools {
      p = new com.cloudowski.pipeline()

      p.buildContainer(REPO, 'cloudowski/fussy')
      p.buildHelm(APP_NAME)
      p.publishChart(APP_NAME,'http://charts.192.168.99.100.nip.io/')
      p.deployChart(APP_NAME, "${APP_NAME}-test", "${APP_NAME}")
      p.deployChart(APP_NAME, 'default', "${APP_NAME}-test")
      p.deployChart(APP_NAME, 'default', "${APP_NAME}-stage")
      p.deployChart(APP_NAME, 'default', "${APP_NAME}-prod")

  }
}
