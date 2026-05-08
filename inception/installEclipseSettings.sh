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

# Installs / updates the project's Eclipse formatter settings into each
# module. Merges formatter keys into existing core.prefs (preserving any
# module-specific compiler/nullness settings) and replaces ui.prefs.

set -eu

JDT_CORE_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.core.prefs"
JDT_UI_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.ui.prefs"

if [ ! -f "$JDT_CORE_PREFS" ] || [ ! -f "$JDT_UI_PREFS" ]; then
  echo "Run from the inception/ directory (next to inception-build/)." >&2
  exit 1
fi

FORMATTER_BLOCK=$(mktemp)
trap 'rm -f "$FORMATTER_BLOCK"' EXIT
grep "^org\.eclipse\.jdt\.core\.formatter\." "$JDT_CORE_PREFS" > "$FORMATTER_BLOCK"

installPrefs() {
  module="$1"
  echo "Updating formatter prefs in $module"
  mkdir -p "$module/.settings"
  prefs="$module/.settings/org.eclipse.jdt.core.prefs"

  if [ -f "$prefs" ]; then
    grep -v "^org\.eclipse\.jdt\.core\.formatter\." "$prefs" > "$prefs.tmp"
  else
    echo "eclipse.preferences.version=1" > "$prefs.tmp"
  fi
  cat "$FORMATTER_BLOCK" >> "$prefs.tmp"
  sort -u -o "$prefs" "$prefs.tmp"
  rm "$prefs.tmp"

  cp "$JDT_UI_PREFS" "$module/.settings/"
}

# Apply to every direct submodule that contains any .java source.
for module in */; do
  module="${module%/}"
  [ "$module" = "inception-build" ] && continue
  if [ -d "$module/src" ] && find "$module/src" -name "*.java" -print -quit 2>/dev/null | grep -q .; then
    installPrefs "$module"
  fi
done
