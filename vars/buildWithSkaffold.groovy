#!/usr/bin/env groovy

def call(creds, registry, repo) {
    withDockerRegistry(credentialsId: creds, url: "https://${registry}") {
        withEnv(["SKAFFOLD_DEFAULT_REPO=${registry}/${repo}"]) {
            sh """#!/bin/bash

                set -o errexit
                set -o pipefail
                set -o xtrace

                rm -f build.json
                export BUILD_DATE="\$(date)"
                docker build .
                skaffold build -q | tee build.json
                """
        }
    }
}

return this
