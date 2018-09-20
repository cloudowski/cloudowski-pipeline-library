#!/usr/bin/env groovy

def download(version='0.11.8'){
  if (!fileExists('terraform')) {
    sh """
      curl -LsSo terraform.zip https://releases.hashicorp.com/terraform/${version}/terraform_${version}_linux_amd64.zip \
        && unzip -o terraform.zip \
        && chmod 755 terraform
    """
  } else {
    println("Terraform already downloaded")
  }
}

def init() {
  exec('init -upgrade')
  exec('get -update=true')
}

def workspace_exists(ws) {
  def status = sh script: "./terraform workspace select ${ws} &> /dev/null", returnStatus: true
  return !status
}

def set_workspace(ws) {
  if (!workspace_exists(ws)) {
    exec("workspace new ${ws}")
  }
  exec("workspace select ${ws}")
}

def exec(command, color = false) {
  def tf_envs = [ "TF_INPUT=0", "TF_IN_AUTOMATION=1"]
  if (color) {
    tf_envs << "TF_CLI_ARGS='-no-color'"
  }

  if (!fileExists('terraform')) {
    download()
  }

  withEnv(tf_envs) {
    //def status = sh script: '#!/bin/sh -ex\n' + "./terraform ${command}", returnStatus: true, returnStdout: !silent
    //return status
    sh "./terraform ${command}"
  }
}


return this;
