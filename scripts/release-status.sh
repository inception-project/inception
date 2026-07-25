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

# Reports what is done and what is still missing for a release, in one pass.
#
# Read-only: it queries git, the file system, the GitHub release, GitHub Pages
# and ghcr.io, and changes nothing. State is derived from those sources rather
# than from a state file, so a release you performed by hand - or on another
# machine - is recognized just the same.
#
# Usage:
#   release-status.sh <version>          e.g. release-status.sh 41.2
#
# Also reachable as `release.sh status <version>`.

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"
# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------
#
# One `report_<area>` function per section of the output. Each resolves the
# state it needs and prints its own findings; none of them change anything.
#
# Where a check cannot reach its source (offline, no credentials) it says so
# with a `-` note rather than reporting the step as missing: "could not tell"
# and "not done" are different answers and conflating them would be misleading.

# Is the DMG notarized and stapled? `stapler validate` exits 0 for a stapled
# DMG and non-zero otherwise -- but it also exits 0 while printing an error
# for a missing file, so the caller must check existence first.
dmg_is_stapled() {
  xcrun stapler validate "$1" >/dev/null 2>&1
}

# --- Git tag ---------------------------------------------------------------

# Resolves the tag state into TAG_LOCAL_SHA / TAG_REMOTE_COMMIT / TAG_REMOTE_OK
# so both the predicate and the reporter can use it without querying twice.
#
# `git ls-remote` exits 0 both for "no such tag" (no output) and, once piped,
# for a failure to reach the remote -- so the two must be told apart by
# capturing output and status separately rather than by exit code alone.
#
# Release tags are annotated, so refs/tags/X reports the *tag object* while
# refs/tags/X^{} reports the commit it points at. Ask for both and prefer the
# peeled line: comparing the tag object against a local commit would flag a
# mismatch on every annotated tag. Peeling remotely also avoids depending on
# the tag object being present in the local object store.
resolve_tag_state() {
  local tag="$1" remote_out
  TAG_LOCAL_SHA=''; TAG_REMOTE_SHA=''; TAG_REMOTE_COMMIT=''; TAG_REMOTE_OK=1

  TAG_LOCAL_SHA="$(git -C "$PROJECT_ROOT" rev-parse -q --verify \
    "refs/tags/$tag^{commit}" 2>/dev/null || true)"

  if remote_out="$(git -C "$PROJECT_ROOT" ls-remote --tags origin \
        "refs/tags/$tag" "refs/tags/$tag^{}" 2>/dev/null)"; then
    TAG_REMOTE_OK=0
    TAG_REMOTE_SHA="$(awk -v t="refs/tags/$tag" '$2 == t {print $1}' <<<"$remote_out")"
    TAG_REMOTE_COMMIT="$(awk -v p="refs/tags/$tag^{}" '$2 == p {print $1}' <<<"$remote_out")"
    # A lightweight tag has no peeled line; there the ref itself is the commit.
    [ -n "$TAG_REMOTE_COMMIT" ] || TAG_REMOTE_COMMIT="$TAG_REMOTE_SHA"
  fi
}

# Done when the tag is on origin. A tag that exists locally but diverges from
# origin is *not* "todo" -- redoing would mean moving a published tag, which
# rewrites history others may have fetched. Report it as a conflict instead and
# let the caller refuse.
# True when a local tag disagrees with origin about which commit is tagged.
tag_diverged() {
  [ -n "$TAG_LOCAL_SHA" ] && [ -n "$TAG_REMOTE_COMMIT" ] \
    && [ "$TAG_REMOTE_COMMIT" != "$TAG_LOCAL_SHA" ]
}

report_tag() {
  local version="$1" tag; tag="$(tag_for "$version")"
  info "${C_BOLD}Git tag${C_RESET}"

  resolve_tag_state "$tag"

  if [ "$TAG_REMOTE_OK" -ne 0 ]; then
    mark_note "could not query origin (offline?)"
  elif [ -n "$TAG_REMOTE_SHA" ]; then
    mark_ok "$tag pushed to origin"
  else
    mark_missing "$tag not pushed to origin"
  fi

  if [ -n "$TAG_LOCAL_SHA" ]; then
    if tag_diverged; then
      mark_missing "local $tag points at a different commit than origin"
      printf '            %slocal:  %s%s\n' "$C_DIM" "$TAG_LOCAL_SHA" "$C_RESET"
      printf '            %sorigin: %s%s\n' "$C_DIM" "$TAG_REMOTE_COMMIT" "$C_RESET"
    else
      mark_note "also present locally (${TAG_LOCAL_SHA:0:9})"
    fi
  elif [ "$TAG_REMOTE_OK" -eq 0 ] && [ -n "$TAG_REMOTE_SHA" ]; then
    # Perfectly normal -- a fresh clone, or the release was cut elsewhere.
    mark_note "not present locally (run: git fetch --tags)"
  fi
  info ""
}

# --- Local build artifacts -------------------------------------------------

# Done when every expected artifact exists. Notarization is reported but does
# not decide the verdict: an un-notarized DMG is a real problem, yet rebuilding
# is not what fixes it, so it must not make the build step look undone.
report_artifacts() {
  info "${C_BOLD}Local artifacts${C_RESET}"
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
  done < <(expected_artifacts "$1")
  info ""
}

# --- Generated example scripts ---------------------------------------------

# Done when every generated example script pins the requested version. See
# generated_scripts() for why the image repository is per-file and why the
# check is placeholder-agnostic.
#
# Emits nothing; report_scripts repeats the matching so it can show detail.
report_scripts() {
  local version="$1"
  info "${C_BOLD}Generated example scripts${C_RESET}"
  local script_name image_repo script_path image_line
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
  info ""
}

# --- GitHub release --------------------------------------------------------

# Fetches the release once into RELEASE_JSON. `gh release view` fails both for
# "no such release" and for auth/network trouble; the two are told apart by
# checking whether gh can reach the API at all.
resolve_release_state() {
  RELEASE_JSON=''; RELEASE_OK=1
  if RELEASE_JSON="$(gh release view "$1" --repo "$REPO" \
        --json isDraft,isPrerelease,assets,url 2>/dev/null)"; then
    RELEASE_OK=0
  fi
}

# Done when the release exists *and* carries every expected asset. A release
# with missing assets is not done: re-running should upload the rest.
report_release() {
  local version="$1" tag; tag="$(tag_for "$version")"
  info "${C_BOLD}GitHub release${C_RESET}"
  resolve_release_state "$tag"
  if [ "$RELEASE_OK" -ne 0 ]; then
    mark_missing "no release for $tag"
    info ""
    return
  fi

  local is_draft url
  is_draft="$(jq -r '.isDraft' <<<"$RELEASE_JSON")"
  url="$(jq -r '.url' <<<"$RELEASE_JSON")"
  if [ "$is_draft" = "true" ]; then
    mark_ok "draft exists"
  else
    mark_ok "${C_BOLD}PUBLISHED${C_RESET}"
  fi
  mark_note "$url"

  info ""
  info "  ${C_BOLD}Attached assets${C_RESET}"
  local label path asset
  while IFS='|' read -r label path asset; do
    if jq -e --arg n "$asset" '.assets[] | select(.name == $n)' \
         <<<"$RELEASE_JSON" >/dev/null 2>&1; then
      mark_ok "$asset"
    else
      mark_missing "$asset"
    fi
  done < <(expected_artifacts "$version")

  # Anything attached that we did not expect is worth surfacing rather than
  # silently ignoring -- it may be a leftover or a manually added file.
  local expected_names extra name
  expected_names="$(expected_artifacts "$version" | cut -d'|' -f3)"
  extra="$(jq -r '.assets[].name' <<<"$RELEASE_JSON" \
    | grep -Fxv -f <(printf '%s\n' "$expected_names") || true)"
  if [ -n "$extra" ]; then
    info ""
    info "  ${C_BOLD}Additional assets${C_RESET}"
    while IFS= read -r name; do
      [ -n "$name" ] && mark_note "$name"
    done <<<"$extra"
  fi
  info ""
}

# --- GitHub Pages ----------------------------------------------------------

# Both checks read the Pages repository through the API, so they need no
# credentials beyond the `gh` login that `status` already requires.

pages_releases_yml() {
  gh api "repos/$PAGES_REPO/contents/$PAGES_RELEASES_YML" --jq '.content' \
    2>/dev/null | base64 -d 2>/dev/null
}

# Recent entries quote the version (- version: "41.2"), older ones do not
# (- version: 0.4.0), so accept either. Commented-out entries do not count:
# the file keeps disabled snapshot/beta blocks around permanently.
releases_yml_lists() {
  grep -Eq "^-[[:space:]]+version:[[:space:]]+\"?${2//./\\.}\"?[[:space:]]*$" <<<"$1"
}

# The per-release documentation copy. Checking for a guide inside the docs
# directory rather than the directory itself, so a half-finished upload does
# not read as done.
report_pages() {
  local version="$1"
  info "${C_BOLD}GitHub Pages${C_RESET} ${C_DIM}($PAGES_REPO)${C_RESET}"

  local yml; yml="$(pages_releases_yml)"
  if [ -z "$yml" ]; then
    mark_note "could not read $PAGES_RELEASES_YML"
  elif releases_yml_lists "$yml" "$version"; then
    mark_ok "$PAGES_RELEASES_YML lists $version"
  else
    mark_missing "$PAGES_RELEASES_YML does not list $version"
  fi

  local docs_path="releases/$version/docs" docs_listing
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
  info ""
}

# --- Docker image ----------------------------------------------------------

# Which repository to look in depends on whether this is a beta; see
# image_repo_for(). Checking the release repository for a beta would report a
# correctly published beta as missing.
#
# Returns the HTTP status of a manifest HEAD, or nothing if no anonymous token
# could be obtained (offline).
ghcr_manifest_status() {
  local repo_path="$1" version="$2" token
  token="$(curl -s "https://ghcr.io/token?scope=repository:$repo_path:pull&service=ghcr.io" \
    | jq -r '.token // empty' 2>/dev/null || true)"
  [ -n "$token" ] || return 1
  curl -s -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer $token" \
    -H 'Accept: application/vnd.oci.image.index.v1+json,application/vnd.docker.distribution.manifest.list.v2+json,application/vnd.docker.distribution.manifest.v2+json' \
    "https://ghcr.io/v2/$repo_path/manifests/$version"
}

report_docker() {
  local version="$1" repo_path image code
  info "${C_BOLD}Docker image${C_RESET}"
  repo_path="$(image_repo_for "$version")"; repo_path="${repo_path#ghcr.io/}"
  image="ghcr.io/$repo_path:$version"
  if is_beta_version "$version"; then
    mark_note "beta - expected in the snapshots repository"
  fi
  if code="$(ghcr_manifest_status "$repo_path" "$version")"; then
    if [ "$code" = "200" ]; then
      mark_ok "$image pushed"
    else
      mark_missing "$image not pushed (HTTP $code)"
    fi
  else
    mark_note "could not query ghcr.io (offline?)"
  fi
  info ""
}

# --- Manual steps ----------------------------------------------------------

# Listed explicitly and never marked ok: these cannot be verified from here,
# and omitting them would let a clean run read as "release complete".
report_manual() {
  info "${C_BOLD}Not checked by this script${C_RESET}"
  mark_note "all issues and PRs resolved/merged"
  mark_note "release announcement written and added to the GitHub release"
  mark_note "stable / community / demo instances updated"
  mark_note "announcement sent to the mailing list"
  info ""
}

# ---------------------------------------------------------------------------
# status
# ---------------------------------------------------------------------------

cmd_status() {
  local version="${1:-}" derived=0
  if [ -z "$version" ]; then
    version="$(version_from_checkout)" || die "\
Usage: release.sh status <version>   (e.g. 41.3)
No version given and none could be derived from target/checkout."
    derived=1
  fi

  require_tool gh
  require_tool jq

  local tag; tag="$(tag_for "$version")"
  local base; base="$(base_version_for "$version")"

  info "${C_BOLD}Release status for $version${C_RESET}  (tag: $tag)"
  if [ "$derived" -eq 1 ]; then
    info "${C_DIM}version taken from target/checkout${C_RESET}"
  fi
  if [ "$base" != "$version" ]; then
    info "${C_DIM}Native installers use base version $base${C_RESET}"
  fi
  info ""

  report_tag "$version"
  report_artifacts "$version"
  report_scripts "$version"
  report_release "$version"
  report_pages "$version"
  report_manual
  report_docker "$version"
}


main() {
  local version="${1:-}"
  case "$version" in
    -h|--help|help)
      info "Usage: release-status.sh [version]   (default: from target/checkout)"
      exit 0
      ;;
  esac
  # With no argument, cmd_status tries target/checkout and fails if there is
  # nothing to derive from -- a forgotten version must not look like success.
  cmd_status "$version"
}

main "$@"
