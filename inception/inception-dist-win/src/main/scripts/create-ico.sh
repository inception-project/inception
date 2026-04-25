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

# Generates a multi-resolution Windows .ico from a source PNG using
# ImageMagick. ImageMagick must be on PATH ('magick' from IM 7+, falling back
# to 'convert' from IM 6).

set -eu

SOURCE_PNG="$1"
OUTPUT_ICO="$2"

if [ ! -f "$SOURCE_PNG" ]; then
  echo "ERROR: Source PNG not found at $SOURCE_PNG"
  exit 1
fi

if command -v magick >/dev/null 2>&1; then
  IM="magick"
elif command -v convert >/dev/null 2>&1; then
  IM="convert"
else
  echo "ERROR: ImageMagick not found on PATH (need 'magick' or 'convert'). Install from https://imagemagick.org/."
  exit 1
fi

echo "Building Windows .ico from $SOURCE_PNG via $IM..."
$IM "$SOURCE_PNG" -define icon:auto-resize=256,128,64,48,32,16 "$OUTPUT_ICO"
echo "Wrote $OUTPUT_ICO"
