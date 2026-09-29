#!/usr/bin/env bash
#
# Licensed to the Technische Universität Darmstadt under one
#  or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The Technische Universität Darmstadt
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Runs the Maven release. Maven prompts for the versions as usual.
#
# Usage: release-maven.sh [--dry-run]

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"

MVN_ARGS=(release:prepare release:perform
  -DpreparationGoals=clean
  -Darguments=-DskipTests\ -DretryFailedDeploymentCount=3)

case "${1:-}" in
  -h|--help|help) info "Usage: release-maven.sh [--dry-run]"; exit 0 ;;
esac

info '  mvn release:prepare release:perform -DpreparationGoals=clean \'
info '    -Darguments="-DskipTests -DretryFailedDeploymentCount=3"'
info ""
# When Maven asks for the next development version after a beta, its suggestion
# (e.g. 41.0-beta-3-SNAPSHOT) is not what this project uses: development goes
# back to the snapshot the beta came from, e.g. 41.0-SNAPSHOT.
info "${C_DIM}Releasing a beta? The next development version stays on the"
info "same snapshot, not Maven's suggested -beta-N+1-SNAPSHOT.${C_RESET}"
info ""

if [ "${1:-}" = "--dry-run" ]; then
  info "${C_DIM}dry run - nothing executed${C_RESET}"
  exit 0
fi

require_tool mvn
cd "$PROJECT_ROOT"
mvn "${MVN_ARGS[@]}"

info ""
info "Now check: release.sh status <version>"
