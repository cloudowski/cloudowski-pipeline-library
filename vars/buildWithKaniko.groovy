/**
 * This pipeline will build and deploy a Docker image with Kaniko
 * https://github.com/GoogleContainerTools/kaniko
 * without needing a Docker host
 *
 * You need to create a jenkins-docker-cfg secret with your docker config
 * as described in
 * https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-secret-in-the-cluster-that-holds-your-authorization-token
 */


def call(body) {

  def pipelineParams= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  env.APPREPO = pipelineParams.appRepo
  env.REGISTRY = pipelineParams.registry
  env.IMGNAME = pipelineParams.imageName
  env.IMGTAG = pipelineParams.imageTag

def label = "kaniko-${UUID.randomUUID().toString()}"

podTemplate(name: 'kaniko', label: label, yaml: """
kind: Pod
metadata:
  name: kaniko
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    imagePullPolicy: Always
    command:
    - /busybox/cat
    tty: true
 """
  //   volumeMounts:
  //     - name: jenkins-docker-cfg
  //       mountPath: /root
  // volumes:
  // - name: jenkins-docker-cfg
  //   projected:
  //     sources:
  //     - secret:
  //         name: regcred
  //         items:
  //           - key: .dockerconfigjson
  //             path: .docker/config.json

  ) {

  node(label) {
    stage('Build with Kaniko') {
      // git 'https://github.com/jenkinsci/docker-jnlp-slave.git'
      git "$APPREPO"
      container(name: 'kaniko', shell: '/busybox/sh') {
        withEnv(['PATH+EXTRA=/busybox:/kaniko']) {
          sh '''#!/busybox/sh
          /kaniko/executor -f `pwd`/Dockerfile -c `pwd` --skip-tls-verify --cache=true --destination=$REGISTRY/$IMGNAME:$IMGTAG
          '''
        }
      }
    }
  }
}
}