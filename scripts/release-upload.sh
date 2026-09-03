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

# Attaches the built artifacts to an existing GitHub release.
#
# Separate from `draft` so the order of the build steps does not matter: create
# the draft whenever, and run this again as artifacts appear (the Windows MSI
# arrives last, via CI). Uploads with --clobber, so re-running replaces rather
# than fails.
#
# Usage: release-upload.sh [version] [--dry-run]

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"

version=''
dry_run=0
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help|help) info "Usage: release-upload.sh [version] [--dry-run]"; exit 0 ;;
    --dry-run) dry_run=1 ;;
    -*) die "Unknown option '$1'." ;;
    *) [ -z "$version" ] || die "Unexpected argument '$1'."; version="$1" ;;
  esac
  shift
done

[ -n "$version" ] || version="$(version_from_checkout)" \
  || die "No version given and none could be derived from target/checkout."

tag="$(tag_for "$version")"

require_tool gh
require_tool jq

release_json="$(gh release view "$tag" --repo "$REPO" --json isDraft,assets,url 2>/dev/null)" \
  || die "No release for $tag - create it first with: release.sh draft"

# Refuse to touch a published release: its assets are what people have already
# been pointed at. Re-attaching to a draft is safe, changing a published release
# is not something to do implicitly.
[ "$(jq -r '.isDraft' <<<"$release_json")" = "true" ] \
  || die "Release $tag is already published - not modifying its assets."

# Only upload what is missing or has changed size; re-uploading a 360 MB MSI that
# is already attached wastes minutes.
to_upload=()
missing=0
while IFS='|' read -r label path asset; do
  if [ ! -f "$path" ]; then
    info "${C_YELLOW}missing${C_RESET} $label ${C_DIM}(not built yet)${C_RESET}"
    missing=$((missing + 1))
    continue
  fi
  remote_size="$(jq -r --arg n "$asset" \
    '.assets[] | select(.name == $n) | .size' <<<"$release_json")"
  local_size="$(wc -c <"$path" | tr -d ' ')"
  if [ -z "$remote_size" ]; then
    info "${C_GREEN}upload${C_RESET}  $asset ($(file_size "$path"))"
    to_upload+=("$path")
  elif [ "$remote_size" != "$local_size" ]; then
    info "${C_GREEN}replace${C_RESET} $asset ${C_DIM}($remote_size -> $local_size bytes)${C_RESET}"
    to_upload+=("$path")
  else
    info "ok      $asset ${C_DIM}(already attached)${C_RESET}"
  fi
done < <(expected_artifacts "$version")

info ""
if [ "${#to_upload[@]}" -eq 0 ]; then
  info "Nothing to upload.$([ "$missing" -gt 0 ] && printf ' %s artifact(s) still missing.' "$missing")"
  exit 0
fi

if [ "$dry_run" -eq 1 ]; then
  info "${C_DIM}dry run - would upload ${#to_upload[@]} file(s)${C_RESET}"
  exit 0
fi

gh release upload "$tag" --repo "$REPO" --clobber "${to_upload[@]}"

info ""
if [ "$missing" -gt 0 ]; then
  info "$missing artifact(s) still missing - run the remaining build steps."
fi
info "Now check: release.sh status $version"
