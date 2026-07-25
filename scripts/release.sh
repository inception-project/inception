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

# The GitHub Pages site lives in a separate repository. It carries the release
# index (`releases.yml`) and a per-release copy of the documentation.
PAGES_REPO="inception-project/inception-project.github.io"
PAGES_RELEASES_YML="_data/releases.yml"

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
# Generated example scripts
# ---------------------------------------------------------------------------

# The admin guide ships downloadable example scripts that must name the version
# being released. Emits "<file>|<image-repo>" for each: the image repository is
# the string the version has to be pinned to, and it is *not* the same for all
# of them -- kubernetes.yml pulls from inception-snapshots.
#
# Deliberately no placeholder spelling here. Asserting that the file names the
# requested version catches an unsubstituted placeholder of any spelling as
# well as a file left over from an earlier version's build. Grepping for one
# literal placeholder would not: the spelling changes from {revnumber} to
# @project.version@ with the fix for #6186.
generated_scripts() {
  printf 'docker-compose.yml|ghcr.io/inception-project/inception\n'
  printf 'docker-compose-mysql8.yml|ghcr.io/inception-project/inception\n'
  printf 'kubernetes.yml|ghcr.io/inception-project/inception-snapshots\n'
}

GENERATED_SCRIPTS_DIR="$WEBAPP_TARGET/generated-docs/admin-guide/scripts"

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
  # The remote tag is what makes a release a release; a purely local tag means
  # the release was never pushed. The local tag is still worth resolving,
  # because a leftover one from an aborted `release:prepare` shadows the real
  # one and would make every local check look at the wrong commit.
  say "${C_BOLD}Git tag${C_RESET}"
  local local_sha='' remote_sha='' remote_commit='' remote_out remote_ok=1
  local_sha="$(git -C "$PROJECT_ROOT" rev-parse -q --verify "refs/tags/$tag^{commit}" \
    2>/dev/null || true)"

  # `git ls-remote` exits 0 both for "no such tag" (no output) and, once piped,
  # for a failure to reach the remote -- so the two must be told apart by
  # capturing output and status separately rather than by exit code alone.
  #
  # Release tags are annotated, so refs/tags/X reports the *tag object* while
  # refs/tags/X^{} reports the commit it points at. Ask for both and prefer the
  # peeled line: comparing the tag object against a local commit would flag a
  # mismatch on every annotated tag. Peeling remotely also avoids depending on
  # the tag object being present in the local object store.
  if remote_out="$(git -C "$PROJECT_ROOT" ls-remote --tags origin \
        "refs/tags/$tag" "refs/tags/$tag^{}" 2>/dev/null)"; then
    remote_ok=0
    remote_sha="$(awk -v t="refs/tags/$tag" '$2 == t {print $1}' <<<"$remote_out")"
    remote_commit="$(awk -v p="refs/tags/$tag^{}" '$2 == p {print $1}' <<<"$remote_out")"
    # A lightweight tag has no peeled line; there the ref itself is the commit.
    [ -n "$remote_commit" ] || remote_commit="$remote_sha"
  fi

  if [ "$remote_ok" -ne 0 ]; then
    mark_note "could not query origin (offline?)"
  elif [ -n "$remote_sha" ]; then
    mark_ok "$tag pushed to origin"
  else
    mark_missing "$tag not pushed to origin"
  fi

  if [ -n "$local_sha" ]; then
    if [ -n "$remote_commit" ] && [ "$remote_commit" != "$local_sha" ]; then
      mark_missing "local $tag points at a different commit than origin"
      printf '            %slocal:  %s%s\n' "$C_DIM" "$local_sha" "$C_RESET"
      printf '            %sorigin: %s%s\n' "$C_DIM" "$remote_commit" "$C_RESET"
    else
      mark_note "also present locally (${local_sha:0:9})"
    fi
  elif [ "$remote_ok" -eq 0 ] && [ -n "$remote_sha" ]; then
    # Perfectly normal -- a fresh clone, or the release was cut elsewhere.
    mark_note "not present locally (run: git fetch --tags)"
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

  # --- Generated example scripts -------------------------------------------
  say "${C_BOLD}Generated example scripts${C_RESET}"
  local script_name image_repo script_path
  while IFS='|' read -r script_name image_repo; do
    script_path="$GENERATED_SCRIPTS_DIR/$script_name"
    if [ ! -f "$script_path" ]; then
      mark_missing "$script_name not generated yet"
      continue
    fi
    # Find the image line for this repository and require the version to appear
    # on it. Matching the whole line rather than just the version keeps other
    # versions mentioned elsewhere in the file from satisfying the check.
    #
    # The version is not necessarily adjacent to the repository name: the
    # compose files wrap both in shell default expansions, as in
    #   image: "${INCEPTION_IMAGE:-<repo>}:${INCEPTION_VERSION:-<version>}"
    # while kubernetes.yml writes "<repo>:<version>" directly. Anchoring on the
    # repository name and then looking for the version covers both.
    #
    # `|| true` because a non-matching grep must not abort the script: this
    # whole block runs under `set -e`.
    local image_line
    image_line="$(grep -F "$image_repo" "$script_path" | head -1 || true)"
    if [ -z "$image_line" ]; then
      mark_missing "$script_name has no $image_repo image line"
      printf '            %s%s%s\n' \
        "$C_DIM" "${script_path#"$PROJECT_ROOT"/}" "$C_RESET"
    elif grep -qF "$version" <<<"$image_line"; then
      mark_ok "$script_name pins $version"
    else
      mark_missing "$script_name does not pin $version"
      # Showing the actual line distinguishes an unsubstituted placeholder from
      # a file left over from an earlier version's build.
      printf '            %sfound: %s%s\n' \
        "$C_DIM" "$(sed 's/^[[:space:]]*//' <<<"$image_line")" "$C_RESET"
      printf '            %s%s%s\n' \
        "$C_DIM" "${script_path#"$PROJECT_ROOT"/}" "$C_RESET"
    fi
  done < <(generated_scripts)
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

  # --- GitHub Pages --------------------------------------------------------
  # Both checks read the Pages repository through the API, so they need no
  # credentials beyond the `gh` login that `status` already requires.
  say "${C_BOLD}GitHub Pages${C_RESET} ${C_DIM}($PAGES_REPO)${C_RESET}"
  local releases_yml
  if releases_yml="$(gh api "repos/$PAGES_REPO/contents/$PAGES_RELEASES_YML" \
        --jq '.content' 2>/dev/null | base64 -d 2>/dev/null)" \
     && [ -n "$releases_yml" ]; then
    # Recent entries quote the version (- version: "41.2"), older ones do not
    # (- version: 0.4.0), so accept either. Commented-out entries do not count:
    # the file keeps disabled snapshot/beta blocks around permanently.
    if grep -Eq "^-[[:space:]]+version:[[:space:]]+\"?${version//./\\.}\"?[[:space:]]*$" \
         <<<"$releases_yml"; then
      mark_ok "$PAGES_RELEASES_YML lists $version"
    else
      mark_missing "$PAGES_RELEASES_YML does not list $version"
    fi
  else
    mark_note "could not read $PAGES_RELEASES_YML"
  fi

  # The per-release documentation copy. Checking for a guide inside the docs
  # directory rather than the directory itself, so a half-finished upload does
  # not read as done.
  local docs_path="releases/$version/docs"
  local docs_listing
  if docs_listing="$(gh api "repos/$PAGES_REPO/contents/$docs_path" \
        --jq '.[].name' 2>/dev/null)" && [ -n "$docs_listing" ]; then
    if grep -qx 'user-guide.html' <<<"$docs_listing"; then
      mark_ok "release documentation uploaded ($docs_path)"
    else
      mark_missing "$docs_path exists but has no user-guide.html"
    fi
  else
    mark_missing "no release documentation at $docs_path"
  fi
  say ""

  # --- Manual steps --------------------------------------------------------
  # Listed explicitly and never marked ok: these cannot be verified from here,
  # and omitting them would let a clean run read as "release complete".
  say "${C_BOLD}Not checked by this script${C_RESET}"
  mark_note "all issues and PRs resolved/merged"
  mark_note "release announcement written and added to the GitHub release"
  mark_note "stable / community / demo instances updated"
  mark_note "announcement sent to the mailing list"
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

Every subcommand is safe to re-run. State is derived from the file system, from
the GitHub release and from the GitHub Pages site, so steps performed by hand
are recognized too.

`status` does not cover every item of the release checklist; the steps it cannot
verify are listed under "Not checked by this script" in its output.

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
