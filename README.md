# cloudowski-pipeline-library
Pipeline library for use with Jenkins pipelines. Contains various vars and groovy classes.


## Terraform steps

There are a couple of terraform steps defined in this library:

* **terraform.download** - downloads terraform into a workspace (version may be defined as parameter)
* **terraform.init** - intialize terraform state
* **terraform.exec** - executes terraform with options set for automated workflows (cause that's where you will use it - no inputs allowed), downloads it first if it's necessary
* **terraform.set_workspace** - set a workspace and creates it if it doesn't exists

## Building containers and publishing Helm Charts

Here is an example of **Jenkinsfile** that will
* build a container image from a repository
* build Helm Charts (helm files must be present in project root directory)
* publish a Helm Chart (chartmuseum must be already deployed)
* use Helm to deploy it on test, stage and prod


```
@Library("cloudowski-pipeline-library") _

import com.cloudowski.*

withKubernetesTools {
    p = new com.cloudowski.pipeline()

    env.REPO = 'https://github.com/cloudowski/fussy-container'
    env.APP_NAME = 'fussy'

    p.buildContainer(REPO, 'cloudowski/fussy')
    p.buildHelm(APP_NAME)
    p.publishChart(APP_NAME,'http://charts.192.168.99.100.nip.io/')

    p.deployChart(APP_NAME, "${APP_NAME}-test", "${APP_NAME}")
    p.deployChart(APP_NAME, "${APP_NAME}-stage", "${APP_NAME}")
    p.deployChart(APP_NAME, "${APP_NAME}-prod", "${APP_NAME}")


}
```

It is possible to also use a predefined pipeline that can be easily deployed without a need to define a global library in jenkins

```
library identifier: 'cloudowski@master', retriever: modernSCM( [$class: 'GitSCMSource', remote: 'https://github.com/cloudowski/cloudowski-pipeline-library'])

helmPipeline {
    appRepo = 'https://github.com/cloudowski/fussy-container'
    appName = 'fussy'
}
```
