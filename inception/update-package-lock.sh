#!/bin/sh
set -e

MVN=${MVN:-'mvn'}
TS_MODULES=`find . -path "*src/main/ts"`

for module in $TS_MODULES ; do
  pushd "$module"
  echo "PWD: $(pwd)"
  rm -f ../ts_template/package-lock.json
  rm -f package-lock.json
  rm -f package.json
  rm -Rf node_modules
  popd

  pushd "$module/../../.."
  ${MVN} clean generate-resources -Dnpm-install-command=install
  
  ORIG_VERSION=$(${MVN} help:evaluate -Dexpression=project.version -q -DforceStdout)
  SEMVER="$(echo "$ORIG_VERSION" | cut -d'-' -f1).0-SNAPSHOT"
  echo "$ORIG_VERSION -> $SEMVER"

  ARTIFACT_ID=$(${MVN} help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
  echo "$ARTIFACT_ID"
  popd
  
  pushd "$module"
  npm audit fix
  popd
  
  pushd "$module"
  cp package-lock.json ../ts_template/package-lock.json
  sed -i '' "s/${SEMVER}/\${semver}/g" ../ts_template/package-lock.json
  sed -i '' "s/${ARTIFACT_ID}/\${project.artifactId}/g" ../ts_template/package-lock.json
  popd
done

