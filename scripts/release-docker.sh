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

# Builds and pushes the multi-arch Docker image from target/checkout.
#
# Usage: release-docker.sh [--dry-run]

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"

dry_run=0
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help|help) info "Usage: release-docker.sh [--dry-run]"; exit 0 ;;
    --dry-run) dry_run=1 ;;
    *) die "Unexpected argument '$1'." ;;
  esac
  shift
done

# Build from target/checkout, not from the working tree: the image tag comes from
# ${project.version}, so building here would push <version>-SNAPSHOT from the
# development poms. The checkout is the tag release:perform checked out, which is
# what carries the released version - so the version is not a parameter, it is
# whatever that tree says it is.
build_dir="$PROJECT_ROOT/target/checkout"
[ -d "$build_dir" ] || die "\
No target/checkout - run the Maven release first (release.sh maven).
The Docker image is built from the checked-out release tag, not the working tree."
version="$(version_from_checkout)" \
  || die "target/checkout has no released version (a SNAPSHOT, or an aborted run?)."

# The image name has to be passed explicitly: the dist-docker profile defaults it
# to inception-snapshots and release-docker does not override it, so omitting it
# would push a release to the snapshots repository. Betas belong there, final
# releases do not - image_repo_for() encodes that.
image_repo="$(image_repo_for "$version")"
warn_if_checkout_published "$version"

MVN_ARGS=(-pl :inception-dist-docker -Pdist-docker,release-docker clean deploy
  "-Ddocker.image.name=$image_repo")

info "  cd target/checkout && mvn ${MVN_ARGS[*]}"
info ""
info "Pushing ${C_BOLD}$image_repo:$version${C_RESET}"
if is_beta_version "$version"; then
  info "${C_DIM}beta - goes to the snapshots repository${C_RESET}"
fi
info ""

if [ "$dry_run" -eq 1 ]; then
  info "${C_DIM}dry run - nothing executed${C_RESET}"
  exit 0
fi

require_tool mvn
require_tool docker
# Warn early rather than after the multi-arch build. Only checks that ghcr.io is
# known to Docker at all; whether the credentials are still valid shows at push.
grep -q '"ghcr.io"' "${DOCKER_CONFIG:-$HOME/.docker}/config.json" 2>/dev/null \
  || info "${C_YELLOW}ghcr.io not found in the Docker config - run: docker login ghcr.io${C_RESET}"

cd "$build_dir"
mvn "${MVN_ARGS[@]}"

info ""
info "Now check: release.sh status $version"
