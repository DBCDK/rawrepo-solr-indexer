#!groovy

def workerNode = "devel8"

void notifyOfBuildStatus(final String buildStatus) {
    final String subject = "${buildStatus}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    final String details = """<p> Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""
    emailext(
            subject: "$subject",
            body: "$details", attachLog: true, compressLog: false,
            mimeType: "text/html",
            recipientProviders: [[$class: "CulpritsRecipientProvider"]]
    )
}

void deploy(String deployEnvironment) {
    dir("deploy") {
        git(url: "gitlab@git-platform.dbc.dk:metascrum/deploy.git", credentialsId: "gitlab-meta")
    }
    sh """
        bash -c '
            virtualenv -p python3 .
            source bin/activate
            pip3 install --upgrade pip
            pip3 install -U -e \"git+https://github.com/DBCDK/mesos-tools.git#egg=mesos-tools\"
            marathon-config-producer rawrepo-solr-indexer-updateservice-${deployEnvironment} --root deploy/marathon --template-keys DOCKER_TAG=${DOCKER_IMAGE_VERSION} -o rawrepo-solr-indexer-updateservice-${deployEnvironment}.json
            marathon-deployer -a ${MARATHON_TOKEN} -b https://mcp1.dbc.dk:8443 deploy rawrepo-solr-indexer-updateservice-${deployEnvironment}.json
        '
	"""
}

pipeline {
    agent { label workerNode }

    tools {
        maven "Maven 3"
    }

    triggers {
        pollSCM("H/03 * * * *")
    }

    options {
        timestamps()
    }

    environment {
        MARATHON_TOKEN = credentials("METASCRUM_MARATHON_TOKEN")
        DOCKER_IMAGE_VERSION = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
        DOCKER_IMAGE_DIT_VERSION = "DIT-${env.BUILD_NUMBER}"
    }

    stages {
        stage("Clean Workspace") {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage("Verify") {
            steps {
                sh "mvn verify pmd:pmd"
                junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
            }
        }

        stage("Publish PMD Results") {
            steps {
                step([$class          : 'hudson.plugins.pmd.PmdPublisher',
                      pattern         : '**/target/pmd.xml',
                      unstableTotalAll: "0",
                      failedTotalAll  : "0"])
            }
        }

        stage("Docker build") {
            when {
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            steps {
                script {
                    def image = docker.build("docker-io.dbc.dk/rawrepo-solr-indexer-updateservice:${DOCKER_IMAGE_VERSION}",
                                                " --label jobname=${env.JOB_NAME}" +
                                                " --label gitcommit=${env.GIT_COMMIT}" +
                                                " --label buildnumber=${env.BUILD_NUMBER}" +
                                                " --label user=isworker" +
                                                " .")
                    image.push()

                    if (env.BRANCH_NAME == 'develop') {
                        sh """
                            docker tag docker-io.dbc.dk/rawrepo-solr-indexer-updateservice:${DOCKER_IMAGE_VERSION} docker-io.dbc.dk/rawrepo-solr-indexer-updateservice:${DOCKER_IMAGE_DIT_VERSION}
                            docker push docker-io.dbc.dk/rawrepo-solr-indexer-updateservice:${DOCKER_IMAGE_DIT_VERSION}
                        """
                    }
                }
            }
        }

        stage("Deploy to staging") {
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'develop'
                }
            }
            steps {
                script {
                    lock('meta-rawrepo-solr-indexer-updateservice-deploy-staging') {
                        echo "Deploy not yet working so skip"
                        #deploy("basismig")
                        #deploy("fbstest")
                    }
                }
            }
        }

        stage("Deploy to prod") {
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'master'
                }
            }
            steps {
                script {
                    lock('meta-rawrepo-solr-indexer-updateservice-deploy-prod') {
                        echo "Deploy not yet working so skip"
                        #deploy("boblebad")
                        #deploy("cisterne")
                    }
                }
            }
        }
    }

    post {
        unstable {
            notifyOfBuildStatus("Build became unstable")
        }
        failure {
            notifyOfBuildStatus("Build failed")
        }
    }
}