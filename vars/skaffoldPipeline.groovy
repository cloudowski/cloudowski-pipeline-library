def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // def gitBranch = env.getEnvironment()['BRANCH_NAME']
    // config['gitBranch'] = git_branch

    rocketchatNotify = config.containsKey('rocketchatNotify')

    ['registry', 'repo'].each {
        if (!config.containsKey(it)) {
            println "ERROR - missing '$it' variable"
            return
        }
    }
    if (!config.containsKey('kubeconfig_creds')) {
        println "INFO - using default 'jenkins-kubeconfig' for k8s access"
        config['kubeconfig_creds'] = 'jenkins-kubeconfig'
    }
    if (!config.containsKey('harbor_creds')) {
        println "INFO - using default 'harbor-creds' for harbor access"
        config['harbor_creds'] = 'harbor-creds'
    }

    pipeline {
        agent none
        options {
            preserveStashes(buildCount: 5)
            buildDiscarder(logRotator(numToKeepStr: '50'))
        }

        stages {
            stage('build') {
                agent { node { label 'docker' } }
                steps {
                    milestone(10)
                    buildWithSkaffold(config.harbor_creds, config.registry, config.repo)
                    stash name: 'build', includes: 'Dockerfile,skaffold.yaml,build.json,scan.sh'
                }
            }

            stage('test') {
                agent { node { label 'docker' } }
                steps {
                    milestone(20)
                    unstash 'build'
                    scanWithHarbor(config.harbor_creds, config.registry, config.repo)
                }
            }

            stage('approve-deploy-stage') {
                agent { node { label 'docker' } }
                options {
                    timeout(time: 10, unit: 'MINUTES')
                }
                input {
                    message 'Deploy to stage?'
                }
                when {
                    beforeAgent true
                    beforeInput true
                    allOf {
                        not { changeRequest() }
                        not { branch 'master' }
                    }
                }

                steps {
                    milestone(30)
                }
            }

            stage('deploy-stage') {
                agent { node { label 'docker' } }
                when {
                    not { changeRequest() }
                }

                environment {
                    KUBECONFIG = credentials("${config.kubeconfig_creds}")
                    SKAFFOLD_NAMESPACE = 'default'
                }

                steps {
                    milestone(40)
                    unstash 'build'
                        sh 'skaffold deploy -a build.json'
                }
            }
        }
        post {
            success {
                script {
                    if (rocketchatNotify) {
                        node('docker') {
                            rocketSend message: "*Build finished :+1:* - ${env.JOB_NAME} (<${env.BUILD_URL}|Open>)",
                                rawMessage: true,
                                color: 'green',
                                avatar: 'https://carlossanchez.files.wordpress.com/2019/03/jenkins-x.png',
                                attachments: [[$class: 'MessageAttachment', color: 'green', text: 'Success!']]
                        }
                    }
                }
            }
            failure {
                script {
                    if (rocketchatNotify) {
                        node('docker') {
                            rocketSend message: "*Build finished :-1:* - ${env.JOB_NAME} (<${env.BUILD_URL}|Open>)",
                                rawMessage: true,
                                avatar: 'https://carlossanchez.files.wordpress.com/2019/03/jenkins-x.png',
                                attachments: [[$class: 'MessageAttachment', color: 'red', text: 'Failed...']]
                        }
                    }
                }
            }
        }
    }
}
