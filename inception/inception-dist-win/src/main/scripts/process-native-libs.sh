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

# Strips non-Windows native binaries (.dylib, .jnilib, .so) from JARs that
# already contain Windows natives (.dll), to slim down the bundle. No
# codesigning step — Authenticode signing of native DLLs is not currently
# performed by this build.

set -eu

INPUT_DIR="$1"

if [ ! -d "$INPUT_DIR" ]; then
  echo "Input directory $INPUT_DIR does not exist. Aborting."
  exit 1
fi

echo "Hunting for native libraries in $INPUT_DIR..."

# Fail loudly if no jars are present — something has gone wrong upstream.
set +e
ls "$INPUT_DIR"/*.jar >/dev/null 2>&1
no_jars=$?
set -e
if [ "$no_jars" -ne 0 ]; then
  echo "No JAR files found in $INPUT_DIR. Aborting."
  exit 1
fi

# Loop through every jar in the staging directory
for jar_file in "$INPUT_DIR"/*.jar; do

  # Quick check to see if the jar contains any Windows native libraries
  if unzip -l "$jar_file" | grep -E "\.dll$" > /dev/null; then
    echo "--> Found Windows native libraries in: $(basename "$jar_file")"

    ABS_JAR=$(cd "$(dirname "$jar_file")"; pwd)/$(basename "$jar_file")

    # Strip native binaries for non-Windows platforms. Only act on jars that
    # already contain Windows natives, so the foreign entries are guaranteed-
    # unused siblings rather than unrelated content.
    foreign_natives=$(unzip -Z1 "$ABS_JAR" 2>/dev/null | grep -E '\.(dylib|jnilib|so|so\.[0-9.]+)$' || true)
    if [ -n "$foreign_natives" ]; then
      printf '%s\n' "$foreign_natives" | while IFS= read -r entry; do
        [ -z "$entry" ] && continue
        echo "    Stripping foreign native: $entry"
        zip -q -d "$ABS_JAR" -- "$entry"
      done
    fi
  fi
done

echo "Native library processing complete."
