/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

def HCD_ANALYTICS_PSIRT_ID = 'PSIRT_PRD0016518'

def getTriggerUser() {
    def userCauses = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
    return userCauses ? userCauses[0].userName : "Automated System"
}

pipeline {
    agent { label 'taas_image_high_spec' }

    parameters {
        string(name: 'REPOSITORY', defaultValue: 'https://github.com/datastax/cassandra-analytics.git', description: 'Repository:')
        string(name: 'BRANCH_NAME', defaultValue: 'trunk', description: 'Branch name:')
        booleanParam(name: 'RELEASE', defaultValue: false, description: 'Release to IBM TaaS JFrog Artifactory')
        password(name: 'TAAS_TOKEN', defaultValue: '', description: 'Personal token from IBM TaaS JFrog Artifactory (can be obtained at https://na.artifactory.swg-devops.com)')
    }

    options {
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        skipDefaultCheckout()
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('checkout') {
            steps {
                cleanWs()
                script {
                    def gitVars = checkout(
                        changelog: false,
                        poll: false,
                        scm: [$class           : 'GitSCM',
                              userRemoteConfigs: [[credentialsId: 'github-riptano-creds',
                                                   url          : params.REPOSITORY]],
                              branches         : [[name: params.BRANCH_NAME]],
                              extensions       : [
                                  [$class: 'CleanBeforeCheckout'],
                                  [$class: 'LocalBranch', localBranch: params.BRANCH_NAME],
                                  [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]],
                        ])
                }
            }
        }

        stage('build') {
            tools {
                jdk('jdk-11')
            }
            steps {
                script {
                    sh(script:'./scripts/build-analytics.sh')
                    archiveArtifacts(artifacts: "cassandra-analytics-core/build/libs/*.jar")
                }
            }
        }

        stage('mend-oss-scan') {
            environment {
                WS_USERKEY = credentials('mend-analytics-service-token')
                WS_APIKEY = credentials('mend-analytics-org-token')
                WS_PRODUCTNAME = "${HCD_ANALYTICS_PSIRT_ID}"
                WS_WSS_URL = 'https://ibmets.whitesourcesoftware.com/agent'
            }
            steps {
                script {
                    sh(script: 'curl --fail --compressed -L https://unified-agent.s3.amazonaws.com/wss-unified-agent.jar -o wss-unified-agent.jar')

                    // https://docs.mend.io/legacy-sca/latest/getting-started-with-the-unified-agent
                    // https://docs.mend.io/legacy-sca/latest/unified-agent-configuration-parameters#UnifiedAgentConfigurationParameters-ConfigurationFileParameters
                    def mendConf = [
                        "resolveAllDependencies=false",
                    ]
                    writeFile(file: 'wss-unified-agent.config', text: mendConf.join('\n'))

                    def projectName = params.BRANCH_NAME
                    sh(script: "java -jar wss-unified-agent.jar -d cassandra-analytics-core/build/libs/ -project '${projectName}' -generateScanReport true")

                    def reportFileName = "${params.BRANCH_NAME}-oss-report"
                    sh(script: "cp whitesource/*-scan_report.json ./${reportFileName}.json")

                    sh(script: """jq -r '["Name","Group","Artifact","Version","Type","SHA1","Licenses","Vulnerabilities"], (.libraries[] | [.name, .groupId, .artifactId, .version, .type, .sha1, ([.licenses[].name] | join(", ")), ([.vulnerabilities[] | "\\(.name) \\(.severity)"] | join(", "))]) | @csv' '${reportFileName}.json' > '${reportFileName}.csv'""")

                    archiveArtifacts(artifacts: "${reportFileName}.*")
                }
            }
        }

        stage('mend-sast-scan') {
            // https://pages.github.ibm.com/Supply-Chain-Security/AppSec-External-Docs/appsec/Mend/SAST/CLI-scanning/#mend-cli-keys
            environment {
                MEND_USER_KEY     = credentials('mend-analytics-service-token')
                MEND_PRODUCTNAME  = "${HCD_ANALYTICS_PSIRT_ID}"
                MEND_ORGNAME      = "Products"
                MEND_EMAIL        = "${HCD_ANALYTICS_PSIRT_ID.toLowerCase()}service_user@ibm.com"
                MEND_PROJECT_NAME = "${params.BRANCH_NAME}"
                MEND_URL          = "https://ibmets.whitesourcesoftware.com"

                MEND_SAST_PATH_EXCLUSIONS=""
            }
            steps {
                script {
                    sh(script: 'curl --fail --compressed -L https://downloads.mend.io/cli/linux_amd64/mend -o mend && chmod +x mend')

                    // https://docs.mend.io/platform/latest/configure-the-mend-cli-for-sast
                    def projectName = params.BRANCH_NAME
                    def reportFileName = "${params.BRANCH_NAME}-sast-report"
                    sh(script: "./mend code -d . --scope 'Products//${env.MEND_PRODUCTNAME}//${projectName}' -e 101 --non-interactive --report --formats 'json,csv' --filename '${reportFileName}' --non-interactive")

                    archiveArtifacts(artifacts: "${reportFileName}.*")
                }
            }
        }

        stage('release') {
            when {
                expression { return params.RELEASE }
            }
            environment {
                M2_SNAPSHOT_REPO = "https://na.artifactory.swg-devops.com/artifactory/datastax-team-hcd-internal-libs-snapshot-maven-local/"
                M2_RELEASE_REPO = "https://na.artifactory.swg-devops.com/artifactory/datastax-team-hcd-internal-libs-release-maven-local/"
                M2_USER = "${getTriggerUser()}"
                M2_PASS = params.TAAS_TOKEN
            }
            tools {
                jdk('jdk-11')
            }
            steps {
                script {
                    sh(script:'./scripts/release.sh')
                }
            }
        }
    }

    post {
        cleanup {
            cleanWs()
            sh(script: 'sudo rm -rf ~/.m2')
        }
    }
}