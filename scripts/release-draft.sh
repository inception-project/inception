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

# Creates the GitHub release as a draft, with the changelog and the usual
# boilerplate, and attaches the built artifacts.
#
# A draft on purpose: review and edit it in the browser before publishing. The
# issue-type emojis past releases carry are not reproduced - GitHub's generated
# changelog has no way to know them - so add those while editing if wanted.
#
# Usage: release-draft.sh [version] [--dry-run]

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"

version=''
dry_run=0
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help|help) info "Usage: release-draft.sh [version] [--dry-run]"; exit 0 ;;
    --dry-run) dry_run=1 ;;
    -*) die "Unknown option '$1'." ;;
    *) [ -z "$version" ] || die "Unexpected argument '$1'."; version="$1" ;;
  esac
  shift
done

[ -n "$version" ] || version="$(version_from_checkout)" \
  || die "No version given and none could be derived from target/checkout."

tag="$(tag_for "$version")"

# The release this one upgrades from, and what the changelog is diffed against.
#
# 'inception-[0-9]*' rather than 'inception-*': the latter also matches the old
# inception-app-* tags, which sort above everything and would produce a changelog
# spanning years.
#
# Betas are skipped for a final release: 41.0's notes say "from 40.6 to 41.0",
# not from 41.0-beta-2, even though the beta sorts higher. For a beta itself the
# preceding beta is the right comparison, so they are kept in that case.
# Picks the greatest tag strictly below this version rather than simply the
# newest one, so re-drafting an older release still compares against the right
# predecessor instead of the latest tag in the repository.
previous_tag() {
  local tags
  tags="$(git -C "$PROJECT_ROOT" tag --list 'inception-[0-9]*' --sort=-v:refname \
    | grep -vFx "$tag")"
  if is_beta_version "$version"; then
    # sort -V treats 41.0 as *lower* than 41.0-beta-1, so the final release of
    # the same base would be picked as a beta's predecessor. Drop it.
    tags="$(grep -vFx "$(tag_for "$(base_version_for "$version")")" <<<"$tags" || true)"
  else
    tags="$(grep -v -- '-beta' <<<"$tags" || true)"
  fi
  # sort -V puts our tag among the others; whatever lands just after it is the
  # next lower version.
  printf '%s\n%s\n' "$tags" "$tag" | sort -Vr | grep -A1 -Fx "$tag" | tail -1
}

# A .0 release carries features; anything else is bugfixes only.
release_kind() {
  case "$version" in
    *-beta*) printf 'This is a beta release.\n' ;;
    *.0) printf 'This is a feature and bug fix release.\n' ;;
    *) printf 'This is a bug fix release.\n' ;;
  esac
}

java_release() {
  sed -n 's|.*<maven.compiler.release>\([0-9]*\)</maven.compiler.release>.*|\1|p' \
    "$PROJECT_ROOT/inception/pom.xml" | head -1
}

prev="$(previous_tag)"
prev_version="${prev#inception-}"
java_ver="$(java_release)"
[ -n "$java_ver" ] || die "Could not read maven.compiler.release from inception/pom.xml."

# GitHub generates the same "## What's Changed" + "Full Changelog" block that
# past releases carry, so it is fetched rather than rebuilt from the git log.
changelog() {
  gh api "repos/$REPO/releases/generate-notes" \
    -f tag_name="$tag" -f previous_tag_name="$prev" --jq '.body' 2>/dev/null \
    || info "(changelog generation failed - fill this in manually)"
}

TEMPLATE="$SCRIPT_DIR/release-notes.md.template"

# Renders the template, replacing {{NAME}} placeholders.
#
# Placeholders rather than a sourced heredoc: the template is prose that gets
# edited as text, and a heredoc would evaluate any $ or backtick it happens to
# contain. Multi-line values are substituted with awk, since sed cannot insert
# newlines portably.
body() {
  [ -f "$TEMPLATE" ] || die "Template not found: $TEMPLATE"

  # The changelog is many lines, and BSD awk rejects newlines in -v values, so
  # it is passed as a file and spliced in line by line.
  local changes_file; changes_file="$(mktemp)"
  # shellcheck disable=SC2064
  trap "rm -f '$changes_file'" RETURN
  changelog >"$changes_file"

  awk -v kind="$(release_kind)" -v changes_file="$changes_file" \
      -v version="$version" -v prev="$prev_version" -v java="$java_ver" '
    {
      gsub(/\{\{RELEASE_KIND\}\}/, kind)
      gsub(/\{\{PREVIOUS_VERSION\}\}/, prev)
      gsub(/\{\{JAVA_VERSION\}\}/, java)
      gsub(/\{\{VERSION\}\}/, version)
      if ($0 == "{{CHANGELOG}}") {
        while ((getline line < changes_file) > 0) print line
        next
      }
      print
    }
  ' "$TEMPLATE"
}

# Artifacts are not attached here - that is `release.sh upload`, which can run
# repeatedly as they appear (the Windows MSI comes last, from CI). Keeping the
# two apart means the build steps and this one can run in any order.
info "Draft ${C_BOLD}INCEpTION $version${C_RESET} ($tag), changelog since $prev"
info ""

if [ "$dry_run" -eq 1 ]; then
  info "${C_DIM}dry run - the body that would be used:${C_RESET}"
  info ""
  body
  exit 0
fi

require_tool gh
require_tool jq
git -C "$PROJECT_ROOT" rev-parse -q --verify "refs/tags/$tag" >/dev/null 2>&1 \
  || die "Tag $tag does not exist - run the Maven release first."

# An existing draft is left alone rather than treated as an error: notes may have
# been edited by hand already, and re-running must not discard that. This is what
# makes `release.sh all` resumable. A published release is a different matter.
if existing="$(gh release view "$tag" --repo "$REPO" --json isDraft,url 2>/dev/null)"; then
  if [ "$(jq -r '.isDraft' <<<"$existing")" = "true" ]; then
    info "A draft for $tag already exists - leaving it untouched."
    info "$(jq -r '.url' <<<"$existing")"
    info "Attach the artifacts with: release.sh upload $version"
    exit 0
  fi
  die "Release $tag is already published - not replacing it."
fi

prerelease=()
is_beta_version "$version" && prerelease=(--prerelease)

gh release create "$tag" --repo "$REPO" --draft --verify-tag \
  --title "INCEpTION $version" --notes-file - "${prerelease[@]}" < <(body)

info ""
info "Draft created. Attach the artifacts with: release.sh upload $version"
gh release view "$tag" --repo "$REPO" --json url --jq '.url'
