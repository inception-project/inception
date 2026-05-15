#!/bin/sh
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
set -e

MVN=${MVN:-'mvn'}
# Do not change the order without a good reason - modules that are imported by other modules must come before them!
TS_MODULES="
./inception-support-bootstrap/src/main/ts
./inception-js-api/src/main/ts
./inception-diam/src/main/ts
./inception-io-tei/src/main/ts
./inception-io-html/src/main/ts
./inception-io-xml/src/main/ts
./inception-brat-editor/src/main/ts
./inception-diam-editor/src/main/ts
./inception-external-editor/src/main/ts
./inception-html-apache-annotator-editor/src/main/ts
./inception-html-recogito-editor/src/main/ts
./inception-pdf-editor2/src/main/ts
./inception-project-export/src/main/ts
./inception-recommendation/src/main/ts
./inception-ui-dashboard-activity/src/main/ts
./inception-ui-kb/src/main/ts
./inception-ui-search/src/main/ts
./inception-ui-scheduling/src/main/ts
./inception-layer-docmetadata/src/main/ts
./inception-assistant-ui/src/main/ts
./inception-workload/src/main/ts
"

TARGET_MODULE="$1"

if [ -n "$TARGET_MODULE" ]; then
  echo "Running only for module: $TARGET_MODULE"
fi

MATCHED=0

for module in $TS_MODULES ; do
  if [ -n "$TARGET_MODULE" ]; then
    if ! echo "$module" | grep -q "$TARGET_MODULE"; then
      continue
    fi
    MATCHED=1
  fi

  echo "🧹 Cleaning up generated files in $module"
  pushd "$module" >/dev/null 2>&1
  rm -f ../ts_template/bun.lock
  rm -f bun.lock
  rm -f package-lock.json
  rm -f package.json
  rm -Rf node_modules

  rm -f .eslintrc.yml
  cp ../../../../inception-build/src/main/resources/inception/eslint.config.mjs eslint.config.mjs
  cp ../../../../inception-build/src/main/resources/inception/vite.config.js vite.config.js
  cp ../../../../inception-build/src/main/resources/inception/tsconfig.json tsconfig.json
  popd >/dev/null 2>&1

  echo "🔄 Building module $module to update bun.lock"
  pushd "$module/../../.." >/dev/null 2>&1
  ${MVN} clean generate-resources -q -Dnpm-install-command=install -Dts-link-phase=generate-resources

  ORIG_VERSION=$(${MVN} help:evaluate -Dexpression=project.version -q -DforceStdout)
  SEMVER="$(echo "$ORIG_VERSION" | cut -d'-' -f1).0-SNAPSHOT"
  echo "ℹ️ Setting version $ORIG_VERSION -> $SEMVER"

  ARTIFACT_ID=$(${MVN} help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
  popd >/dev/null 2>&1

  echo "🔎 Auditing module $module"
  pushd "$module" >/dev/null 2>&1
  if command -v osv-scanner >/dev/null 2>&1; then
    osv-scanner scan source --lockfile=bun.lock --config=../../../../osv-scanner.toml || AUDIT_FAILED=1
  else
    echo "⚠️  osv-scanner not found on PATH — skipping audit. Install via 'brew install osv-scanner' or see https://google.github.io/osv-scanner/installation/"
  fi
  popd >/dev/null 2>&1

  echo "📦 Updating template bun.lock for module $module"
  pushd "$module" >/dev/null 2>&1
  cp bun.lock ../ts_template/bun.lock
  sed -i '' "s/${SEMVER}/\${semver}/g" ../ts_template/bun.lock
  sed -i '' "s/${ARTIFACT_ID}/\${project.artifactId}/g" ../ts_template/bun.lock
  popd >/dev/null 2>&1
done

if [ -n "$TARGET_MODULE" ] && [ "$MATCHED" -eq 0 ]; then
  echo "ERROR: module '$TARGET_MODULE' not found in TS_MODULES" >&2
  exit 2
fi

if [ -n "$AUDIT_FAILED" ]; then
  echo ""
  echo "⚠️  osv-scanner reported vulnerabilities in one or more modules."
  echo "    Address each finding by either:"
  echo "    - upgrading the affected dependency in package.json (and re-running this script), or"
  echo "    - adding an [[IgnoredVulns]] entry to inception/osv-scanner.toml with a reason"
  echo "      and an ignoreUntil date so the decision will be re-evaluated."
  exit 1
fi
