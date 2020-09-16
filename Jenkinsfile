#!groovy

def workerNode = "devel10"

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

        stage("DIT test") {
            steps {
                script {
                    sh "rm -rf core"
                }
                dir("core") {
                    git(credentialsId: 'gitlab-meta', url: "gitlab@gitlab.dbc.dk:pu/dit-core.git")
                    script {
                        sh """
                            bash -c '
                                source bin/activate ../
                                run --junit -s VERIFIED
                            '
                           """
                        junit "reports/*.xml"
                    }
                }
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
                }
            }
        }

        stage("Update deployments") {
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
                            set-new-version rawrepo-solr-indexer.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-solr-indexer-deployer ${DOCKER_IMAGE_VERSION} -b metascrum-staging
                            set-new-version rawrepo-solr-indexer.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-solr-indexer-deployer ${DOCKER_IMAGE_VERSION} -b basismig
                            set-new-version rawrepo-solr-indexer.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-solr-indexer-deployer ${DOCKER_IMAGE_VERSION} -b fbstest

                            set-new-version services/rawrepo-solr/rawrepo-solr-indexer-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets ${DOCKER_IMAGE_VERSION} -b master
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