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

# Release helper. Every subcommand is independently invocable and safe to
# re-run: state is derived by looking at the file system and at the GitHub
# release, never from a state file. That way you can stop after any step,
# continue days later, and steps you performed by hand are still recognized.
#
# Usage:
#   release.sh status <version>
#
# See `release.sh --help`.

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

REPO="inception-project/inception"

# Resolve the project root from this script's location so the script works
# regardless of the current working directory.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

MACOS_TARGET="$PROJECT_ROOT/inception/inception-dist-macos/target"
WIN_TARGET="$PROJECT_ROOT/inception/inception-dist-win/target"
WEBAPP_TARGET="$PROJECT_ROOT/inception/inception-app-webapp/target"

# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_BOLD=$'\033[1m'; C_DIM=$'\033[2m'
  C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'
else
  C_RESET=''; C_BOLD=''; C_DIM=''; C_GREEN=''; C_YELLOW=''; C_RED=''
fi

say() { printf '%s\n' "$*"; }
warn() { printf '%s\n' "${C_YELLOW}WARNING:${C_RESET} $*" >&2; }
die() { printf '%s\n' "${C_RED}ERROR:${C_RESET} $*" >&2; exit 1; }

# Status markers. Deliberately ASCII-safe words rather than symbols only, so
# the output stays readable when redirected to a file or a CI log.
mark_ok()      { printf '  %sok%s      %s\n' "$C_GREEN" "$C_RESET" "$*"; }
mark_missing() { printf '  %smissing%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
mark_note()    { printf '  %s-%s       %s\n' "$C_DIM" "$C_RESET" "$*"; }

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

require_tool() {
  command -v "$1" >/dev/null 2>&1 || die "Required tool '$1' not found in PATH."
}

# Print the size of a file in a human readable form, or nothing if absent.
file_size() {
  if [ -f "$1" ]; then
    du -h "$1" | awk '{print $1}'
  fi
}

# The git tag used for a given version.
tag_for() { printf 'inception-%s\n' "$1"; }

# Native installers are named after the base version *without* any beta
# suffix, e.g. the inception-41.0-beta-2 release contains
# INCEpTION-41.0-x86_64.msi, while the JAR keeps the full version.
base_version_for() { printf '%s\n' "${1%%-beta*}"; }

# ---------------------------------------------------------------------------
# Expected artifacts
# ---------------------------------------------------------------------------

# Emits "<label>|<local-path>|<asset-name>" for every artifact a release is
# expected to carry. This single definition is what `status` checks and what
# later subcommands (draft/upload) will consume, so the notion of "complete"
# lives in exactly one place.
expected_artifacts() {
  local version="$1"
  local base; base="$(base_version_for "$version")"

  printf 'macOS DMG (Apple Silicon)|%s|%s\n' \
    "$MACOS_TARGET/jpackage-aarch64/INCEpTION-$base-aarch64.dmg" \
    "INCEpTION-$base-aarch64.dmg"
  printf 'macOS DMG (Intel)|%s|%s\n' \
    "$MACOS_TARGET/jpackage-x86_64/INCEpTION-$base-x86_64.dmg" \
    "INCEpTION-$base-x86_64.dmg"
  printf 'Windows MSI|%s|%s\n' \
    "$WIN_TARGET/jpackage-x86_64/INCEpTION-$base-x86_64.msi" \
    "INCEpTION-$base-x86_64.msi"
  printf 'Executable JAR|%s|%s\n' \
    "$WEBAPP_TARGET/inception-app-webapp-$version-standalone.jar" \
    "inception-app-webapp-$version-standalone.jar"
  printf 'JAR signature|%s|%s\n' \
    "$WEBAPP_TARGET/inception-app-webapp-$version-standalone.jar.asc" \
    "inception-app-webapp-$version-standalone.jar.asc"
}

# ---------------------------------------------------------------------------
# status
# ---------------------------------------------------------------------------

# Is the DMG notarized and stapled? `stapler validate` exits 0 for a stapled
# DMG and non-zero otherwise -- but it also exits 0 while printing an error
# for a missing file, so the caller must check existence first.
dmg_is_stapled() {
  xcrun stapler validate "$1" >/dev/null 2>&1
}

cmd_status() {
  local version="${1:-}"
  [ -n "$version" ] || die "Usage: release.sh status <version>   (e.g. 41.3)"

  require_tool gh
  require_tool jq

  local tag; tag="$(tag_for "$version")"
  local base; base="$(base_version_for "$version")"

  say "${C_BOLD}Release status for $version${C_RESET}  (tag: $tag)"
  if [ "$base" != "$version" ]; then
    say "${C_DIM}Native installers use base version $base${C_RESET}"
  fi
  say ""

  # --- Git tag -------------------------------------------------------------
  say "${C_BOLD}Git tag${C_RESET}"
  if git -C "$PROJECT_ROOT" rev-parse -q --verify "refs/tags/$tag" >/dev/null 2>&1; then
    mark_ok "$tag exists locally"
  else
    mark_missing "$tag does not exist locally"
  fi
  say ""

  # --- Local build artifacts ----------------------------------------------
  say "${C_BOLD}Local artifacts${C_RESET}"
  local label path asset size
  while IFS='|' read -r label path asset; do
    if [ -f "$path" ]; then
      size="$(file_size "$path")"
      case "$path" in
        *.dmg)
          if dmg_is_stapled "$path"; then
            mark_ok "$label ($size, notarized)"
          else
            mark_ok "$label ($size, ${C_YELLOW}not notarized${C_RESET})"
          fi
          ;;
        *) mark_ok "$label ($size)" ;;
      esac
    else
      mark_missing "$label"
      printf '            %sexpected: %s%s\n' "$C_DIM" "${path#"$PROJECT_ROOT"/}" "$C_RESET"
    fi
  done < <(expected_artifacts "$version")
  say ""

  # --- Docker Compose file -------------------------------------------------
  say "${C_BOLD}Docker Compose file${C_RESET}"
  local compose="$WEBAPP_TARGET/generated-docs/admin-guide/scripts/docker-compose.yml"
  if [ -f "$compose" ]; then
    if grep -q '{revnumber}' "$compose"; then
      mark_missing "contains unsubstituted {revnumber} placeholder"
      printf '            %s%s%s\n' "$C_DIM" "${compose#"$PROJECT_ROOT"/}" "$C_RESET"
    else
      mark_ok "generated ($(file_size "$compose"))"
    fi
  else
    mark_missing "not generated yet"
  fi
  say ""

  # --- GitHub release ------------------------------------------------------
  say "${C_BOLD}GitHub release${C_RESET}"
  local release_json
  if release_json="$(gh release view "$tag" --repo "$REPO" \
        --json isDraft,isPrerelease,assets,url 2>/dev/null)"; then
    local is_draft url
    is_draft="$(jq -r '.isDraft' <<<"$release_json")"
    url="$(jq -r '.url' <<<"$release_json")"
    if [ "$is_draft" = "true" ]; then
      mark_ok "draft exists"
    else
      mark_ok "${C_BOLD}PUBLISHED${C_RESET}"
    fi
    mark_note "$url"

    say ""
    say "  ${C_BOLD}Attached assets${C_RESET}"
    while IFS='|' read -r label path asset; do
      if jq -e --arg n "$asset" '.assets[] | select(.name == $n)' \
           <<<"$release_json" >/dev/null 2>&1; then
        mark_ok "$asset"
      else
        mark_missing "$asset"
      fi
    done < <(expected_artifacts "$version")

    # Anything attached that we did not expect is worth surfacing rather than
    # silently ignoring -- it may be a leftover or a manually added file.
    local expected_names extra
    expected_names="$(expected_artifacts "$version" | cut -d'|' -f3)"
    extra="$(jq -r '.assets[].name' <<<"$release_json" \
      | grep -Fxv -f <(printf '%s\n' "$expected_names") || true)"
    if [ -n "$extra" ]; then
      say ""
      say "  ${C_BOLD}Additional assets${C_RESET}"
      while IFS= read -r name; do
        [ -n "$name" ] && mark_note "$name"
      done <<<"$extra"
    fi
  else
    mark_missing "no release for $tag"
  fi
  say ""

  # --- Docker image --------------------------------------------------------
  say "${C_BOLD}Docker image${C_RESET}"
  local image="ghcr.io/inception-project/inception:$version"
  local token
  token="$(curl -s "https://ghcr.io/token?scope=repository:inception-project/inception:pull&service=ghcr.io" \
    | jq -r '.token // empty' 2>/dev/null || true)"
  if [ -n "$token" ]; then
    local code
    code="$(curl -s -o /dev/null -w '%{http_code}' \
      -H "Authorization: Bearer $token" \
      -H 'Accept: application/vnd.oci.image.index.v1+json,application/vnd.docker.distribution.manifest.list.v2+json,application/vnd.docker.distribution.manifest.v2+json' \
      "https://ghcr.io/v2/inception-project/inception/manifests/$version")"
    if [ "$code" = "200" ]; then
      mark_ok "$image pushed"
    else
      mark_missing "$image not pushed (HTTP $code)"
    fi
  else
    mark_note "could not query ghcr.io (offline?)"
  fi
  say ""
}

# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

usage() {
  cat <<'EOF'
Release helper for INCEpTION.

Usage:
  release.sh status <version>    Show what is done and what is still missing
  release.sh --help             Show this help

Every subcommand is safe to re-run. State is derived from the file system and
from the GitHub release, so steps performed by hand are recognized too.

Examples:
  release.sh status 41.2
EOF
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    status) shift; cmd_status "$@" ;;
    -h|--help|help|'') usage ;;
    *) die "Unknown subcommand '$cmd'. Try --help." ;;
  esac
}

main "$@"
