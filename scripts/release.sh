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

# Release helper for INCEpTION - dispatcher only.
#
# Each subcommand lives in its own script next to this one, so that any single
# step can be read and understood on its own:
#
#   status  ->  release-status.sh
#   maven   ->  release-maven.sh
#   macos   ->  release-macos.sh
#   docker  ->  release-docker.sh
#   windows ->  release-windows.sh
#   draft   ->  release-draft.sh
#   upload  ->  release-upload.sh
#   all     ->  release-all.sh   (runs the above in order)
#
# Shared definitions live in release-functions.shlib. This file only picks a
# script and hands over; it contains no release logic itself.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
  cat <<'USAGE'
Release helper for INCEpTION.

Usage:
  release.sh status [version]     Show what is done and what is still missing
  release.sh maven                Run the Maven release (prepare + perform)
  release.sh macos                Build and notarize the macOS DMGs
  release.sh docker               Build and push the Docker image
  release.sh windows              Build the Windows MSI on CI and fetch it
  release.sh draft [version]      Create the GitHub release as a draft
  release.sh upload [version]     Attach the built artifacts to the draft
  release.sh all                  Run all of the above, in order
  release.sh --help              Show this help

Subcommands are separate scripts in the same directory and can also be called
directly:

  release-status.sh [version]
  release-maven.sh [--dry-run]
  release-macos.sh [--dry-run]
  release-docker.sh [--dry-run]
  release-windows.sh [--rebuild] [--dry-run]
  release-draft.sh [version] [--dry-run]
  release-upload.sh [version] [--dry-run]
  release-all.sh [--from <step>] [--dry-run]

`macos`, `docker` and `windows` take the version from target/checkout - the tag
release:perform checks out - because artifact names come from the pom version and
the working tree is still on a SNAPSHOT. Run them after `maven`, in the same
working copy. `status` and `draft` read that version too when none is given.

`status` is read-only; the checklist items it cannot verify are listed under
"Not checked by this script" in its output.

`maven` runs release:prepare and release:perform; Maven prompts for the
versions as usual.

`docker` derives the image name from the version: betas go to
inception-snapshots, final releases to inception.

`windows` dispatches the dist-win workflow on the release tag (the MSI cannot be
built locally), waits for it and unpacks the artifact where the other steps
expect it. It reuses an existing successful run unless --rebuild is given, so
re-running it is cheap. Requires the tag to be pushed.

`draft` creates the release as a draft with GitHub's generated changelog and the
usual boilerplate. It does not attach artifacts; issue-type emojis are not added.

`upload` attaches the artifacts to the draft and can be re-run as they appear -
the Windows MSI arrives last, from CI. It skips what is already attached at the
same size, and refuses to modify a release that has been published. Because the
two are separate, `draft` and the build steps can run in any order.

`all` runs maven, macos, windows, docker, draft and upload in that order and
stops at the first failure. Every step is re-runnable, so fix whatever broke and
run it again - or skip ahead with --from <step>. It leaves the release as a
draft for you to review and publish.

Examples:
  release.sh status 41.2
  release.sh all --dry-run
  release.sh all --from docker
USAGE
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    status) shift; exec "$SCRIPT_DIR/release-status.sh" "$@" ;;
    maven)  shift; exec "$SCRIPT_DIR/release-maven.sh" "$@" ;;
    macos)  shift; exec "$SCRIPT_DIR/release-macos.sh" "$@" ;;
    docker) shift; exec "$SCRIPT_DIR/release-docker.sh" "$@" ;;
    windows) shift; exec "$SCRIPT_DIR/release-windows.sh" "$@" ;;
    draft)  shift; exec "$SCRIPT_DIR/release-draft.sh" "$@" ;;
    upload) shift; exec "$SCRIPT_DIR/release-upload.sh" "$@" ;;
    all)    shift; exec "$SCRIPT_DIR/release-all.sh" "$@" ;;
    -h|--help|help|'') usage ;;
    *)
      printf 'ERROR: Unknown subcommand %s. Try --help.\n' "'$cmd'" >&2
      exit 1
      ;;
  esac
}

main "$@"
