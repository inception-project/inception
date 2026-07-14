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

# Runs the Prettier auto-formatter (the "format" npm script) across the
# TypeScript modules. Unlike update-bun-lock.sh the module list is auto-detected
# here (formatting order does not matter): every */src/main/ts module is
# included.
#
# Pass a module name (or substring) as the first argument to format only that
# module; omit it to format all of them. Pass --check to report unformatted
# files without writing (uses the "format:check" script).

cd "$(dirname "$0")"

SCRIPT="format"
TARGET_MODULE=""

for arg in "$@" ; do
  case "$arg" in
    --check)
      SCRIPT="format:check"
      ;;
    -*)
      echo "ERROR: unknown option '$arg'" >&2
      echo "Usage: $0 [--check] [module]" >&2
      exit 2
      ;;
    *)
      TARGET_MODULE="$arg"
      ;;
  esac
done

# Legacy/retired modules that are no longer part of the active build and are
# intentionally not kept formatter-clean. These are skipped entirely.
EXCLUDED_MODULES="
./inception-pdf-editor/src/main/ts
"

# Auto-detect TS modules: any */src/main/ts that has a package.json.
# Sorted for stable output; order is irrelevant for formatting.
TS_MODULES=$(
  for pkg in ./*/src/main/ts/package.json ; do
    [ -f "$pkg" ] || continue
    dir=$(dirname "$pkg")
    excluded=0
    for ex in $EXCLUDED_MODULES ; do
      if [ "$dir" = "$ex" ]; then
        excluded=1
        break
      fi
    done
    [ "$excluded" -eq 0 ] && echo "$dir"
  done | sort
)

if [ -z "$TS_MODULES" ]; then
  echo "ERROR: no */src/main/ts modules found" >&2
  exit 2
fi

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

  if ! grep -q "\"$SCRIPT\"[[:space:]]*:" "$module/package.json" ; then
    echo "❌ $module has no '$SCRIPT' script in package.json — please add one"
    FAILED=1
    continue
  fi

  if [ ! -d "$module/node_modules" ]; then
    echo "⚠️  Skipping $module — no node_modules (run a build first to install dependencies)"
    continue
  fi

  echo "🧹 Formatting $module"
  pushd "$module" >/dev/null 2>&1
  npm run "$SCRIPT" || FAILED=1
  popd >/dev/null 2>&1
done

if [ -n "$TARGET_MODULE" ] && [ "$MATCHED" -eq 0 ]; then
  echo "ERROR: module '$TARGET_MODULE' not found among the TypeScript modules" >&2
  exit 2
fi

if [ -n "$FAILED" ]; then
  echo ""
  echo "⚠️  One or more modules reported problems (missing '$SCRIPT' script or formatting issues)."
  exit 1
fi
