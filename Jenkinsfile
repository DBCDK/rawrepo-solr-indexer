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
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
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

        stage("Archive artifacts") {
            steps {
                archiveArtifacts(artifacts: "target/rawrepo-solr-indexer-2.0-SNAPSHOT-solr-config.zip")
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
                    // Build and push indexer
                    def image = docker.build("docker-io.dbc.dk/rawrepo-solr-indexer:${DOCKER_IMAGE_VERSION}",
                                                " --label jobname=${env.JOB_NAME}" +
                                                " --label gitcommit=${env.GIT_COMMIT}" +
                                                " --label buildnumber=${env.BUILD_NUMBER}" +
                                                " --label user=isworker" +
                                                " .")
                    image.push()

                    // Build and push solr server
                	sh "rm -rf solr/docker/rawrepo-solr-indexer-solr-config-zip"
	                sh "unzip target/rawrepo-solr-indexer-2.0-SNAPSHOT-solr-config.zip -d solr/docker/rawrepo-solr-indexer-solr-config-zip"

                    def solr = docker.build("docker-io.dbc.dk/rawrepo-solr-server:${DOCKER_IMAGE_VERSION}",
                                                " --label jobname=${env.JOB_NAME}" +
                                                " --label gitcommit=${env.GIT_COMMIT}" +
                                                " --label buildnumber=${env.BUILD_NUMBER}" +
                                                " --label user=isworker" +
                                                " solr/docker")
                    solr.push()

                    if (env.BRANCH_NAME == 'master') {
                        sh """
                            docker tag docker-io.dbc.dk/rawrepo-solr-indexer:${DOCKER_IMAGE_VERSION} docker-io.dbc.dk/rawrepo-solr-indexer:${DOCKER_IMAGE_DIT_VERSION}
                            docker push docker-io.dbc.dk/rawrepo-solr-indexer:${DOCKER_IMAGE_DIT_VERSION}

                            docker tag docker-io.dbc.dk/rawrepo-solr-server:${DOCKER_IMAGE_VERSION} docker-io.dbc.dk/rawrepo-solr-server:${DOCKER_IMAGE_DIT_VERSION}
                            docker push docker-io.dbc.dk/rawrepo-solr-server:${DOCKER_IMAGE_DIT_VERSION}
                        """
                    }
                }
            }
        }

        stage("Update DIT") {
            agent {
                docker {
                    label workerNode
                    image "docker.dbc.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'master'
                }
            }
            steps {
                script {
                    dir("deploy") {
                        sh """
                            set-new-version services/rawrepo-solr ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_IMAGE_DIT_VERSION} -b master
						"""
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