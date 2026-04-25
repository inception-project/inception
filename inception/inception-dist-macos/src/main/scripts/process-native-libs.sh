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

set -eu

INPUT_DIR="$1"
SIGNING_IDENTITY="$2"

if [ -z "$SIGNING_IDENTITY" ]; then
  echo "No signing identity provided. Skipping native library signing."
  exit 0
fi

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

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT INT TERM

# Loop through every jar in the staging directory
for jar_file in "$INPUT_DIR"/*.jar; do

  # Quick check to see if the jar contains any native macOS libraries
  if unzip -l "$jar_file" | grep -E "\.dylib$|\.jnilib$" > /dev/null; then
    echo "--> Found native libraries in: $(basename "$jar_file")"

    # Get absolute path to the jar file to avoid relative path issues in subshells
    ABS_JAR=$(cd "$(dirname "$jar_file")"; pwd)/$(basename "$jar_file")

    # Strip native binaries for non-macOS platforms to slim down the bundle.
    # Only act on jars that already contain macOS natives, so we know these
    # entries are guaranteed-unused siblings rather than unrelated content.
    foreign_natives=$(unzip -Z1 "$ABS_JAR" 2>/dev/null | grep -E '\.(so|so\.[0-9.]+|dll|lib|pdb)$' || true)
    if [ -n "$foreign_natives" ]; then
      printf '%s\n' "$foreign_natives" | while IFS= read -r entry; do
        [ -z "$entry" ] && continue
        echo "    Stripping foreign native: $entry"
        zip -q -d "$ABS_JAR" -- "$entry"
      done
    fi

    # Extract just the native libraries into our temp folder
    # Build a list of entries in the jar that match .dylib or .jnilib
    native_files=$(unzip -Z1 "$ABS_JAR" 2>/dev/null | grep -E '\.dylib$|\.jnilib$' || true)
    if [ -z "$native_files" ]; then
      echo "    No matching native files found in $(basename "$jar_file")"
      continue
    fi

    # Read entries line-by-line to handle spaces safely
    # Preserve directory structure to avoid filename collisions and
    # use -o to overwrite without prompting (non-interactive)
    printf '%s\n' "$native_files" | while IFS= read -r entry; do
      [ -z "$entry" ] && continue
      echo "    Extracting: $entry"
      unzip -q -o "$ABS_JAR" "$entry" -d "$TMP_DIR"
    done

    # Find all extracted libs and codesign them
    find "$TMP_DIR" -type f \( -name "*.dylib" -o -name "*.jnilib" \) -exec sh -c '
      identity=$1; shift
      for dylib do
        echo "    Signing: $(basename "$dylib")"
        codesign --force -s "$identity" --options runtime --timestamp "$dylib"
      done
    ' sh "$SIGNING_IDENTITY" {} +

    # Update the original jar with the signed files. Iterate the entry names
    # we got from unzip -Z1 (no leading './') and feed them to zip from inside
    # TMP_DIR so the archive entries match the originals exactly — otherwise
    # 'find . -exec zip' would add './'-prefixed duplicate entries.
    printf '%s\n' "$native_files" | while IFS= read -r entry; do
      [ -z "$entry" ] && continue
      (cd "$TMP_DIR" && zip -q -u "$ABS_JAR" -- "$entry")
    done

    # Clean up the temp directory for the next jar
    rm -rf "$TMP_DIR"/*
  fi
done

echo "Native library processing complete."
