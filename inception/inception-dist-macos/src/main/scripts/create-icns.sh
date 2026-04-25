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

SOURCE_PNG="$1"
OUTPUT_ICNS="$2"

if [ ! -f "$SOURCE_PNG" ]; then
  echo "ERROR: Source PNG not found at $SOURCE_PNG"
  exit 1
fi

ICONSET_DIR="${OUTPUT_ICNS%.icns}.iconset"

echo "Building macOS iconset from $SOURCE_PNG..."
mkdir -p "$ICONSET_DIR"

# sips chatters file paths to stdout on success; redirect to keep the build log readable.
for spec in "16:icon_16x16" "32:icon_16x16@2x" "32:icon_32x32" "64:icon_32x32@2x" \
            "128:icon_128x128" "256:icon_128x128@2x" "256:icon_256x256" \
            "512:icon_256x256@2x" "512:icon_512x512" "1024:icon_512x512@2x"; do
  size=${spec%%:*}
  name=${spec#*:}
  echo "  -> ${name}.png (${size}x${size})"
  sips -z "$size" "$size" "$SOURCE_PNG" --out "$ICONSET_DIR/${name}.png" >/dev/null
done

echo "Compiling iconset to $OUTPUT_ICNS..."
iconutil -c icns "$ICONSET_DIR" -o "$OUTPUT_ICNS"

rm -rf "$ICONSET_DIR"
echo "Wrote $OUTPUT_ICNS"
