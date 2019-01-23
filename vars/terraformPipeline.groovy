def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent none
        tools {
            terraform '0.13'
        }
        options {
            preserveStashes(buildCount: 5)
            buildDiscarder(logRotator(numToKeepStr: '50'))
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        environment {
            // KUBECONFIG = credentials("${config.kubeconfig_creds}")
            TF_IN_AUTOMATION = '1'
            TF_INPUT = '0'
        }

        stages {
            stage('plan') {
                agent { node { label 'docker' } }
                steps {
                    sh '''
                        terraform init
                        terraform plan -out tf.plan
                    '''
                    stash name: 'plan', includes: 'tf.plan, .terraform'
                }
            }

            stage('approve-apply') {
                agent { node { label 'docker' } }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                input {
                    message 'Apply?'
                }
                when {
                    beforeAgent true
                    beforeInput true
                    allOf {
                        not { changeRequest() }
                        // not { branch 'master' }
                    }
                }
                steps {
                    milestone(10)
                }
            }

            stage('apply') {
                agent { node { label 'docker' } }
                when {
                    allOf {
                        not { changeRequest() }
                        // not { branch 'master' }
                    }
                }
                steps {
                    unstash 'plan'
                    sh 'terraform apply tf.plan'
                }
            }
        }
    }
}
