#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Script uploads artefacts to Maven repository. Required environment variables:
#   - M2_SNAPSHOT_REPO: URL to snapshot repository
#   - M2_RELEASE_REPO:  URL to release repository
#   - M2_USER:          Username
#   - M2_PASS:          Password

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )

CASSANDRA_ANALYTICS_VERSION=$(cat ${SCRIPT_DIR}/../gradle.properties | grep 'version=' | awk -F'=' '{print $2}')

PUBLISH_REPO="${M2_RELEASE_REPO}"
if [[ $CASSANDRA_ANALYTICS_VERSION == *-SNAPSHOT ]]; then
  PUBLISH_REPO="${M2_SNAPSHOT_REPO}"
fi

echo "Releasing version ${CASSANDRA_ANALYTICS_VERSION} to ${PUBLISH_REPO}"

env maven.repository.url="${PUBLISH_REPO}" maven.username="${M2_USER}" maven.password="${M2_PASS}" bash << 'EOF'
  ./gradlew cassandra-analytics-common:publish -PartifactType=common
  ./gradlew cassandra-analytics-sidecar-client:publish -PartifactType=common

  for scala_version in '2.12' '2.13'; do
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-spark-converter:publish -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-bridge:publish -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-core:publish -PartifactType=spark
  done
EOF