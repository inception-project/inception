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

# Builds and notarizes the macOS DMGs from target/checkout.
#
# Usage: release-macos.sh [--dry-run]

set -euo pipefail

# shellcheck source=release-functions.shlib
source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/release-functions.shlib"

# One invocation, not two: notarization binds to `verify`, which runs `package`
# first, so building and notarizing separately would build the DMGs twice. Both
# profiles are needed here - release-macos only adds the notarize executions and
# does not activate dist-macos.
#
# mac.signing.identity and mac.notarization.keychain.profile are enforced by the
# poms but come from the macos-signing profile in ~/.m2/settings.xml, so they are
# deliberately not passed here.
MVN_ARGS=(-pl :inception-dist-macos verify -Pdist-macos,release-macos)

case "${1:-}" in
  -h|--help|help) info "Usage: release-macos.sh [--dry-run]"; exit 0 ;;
esac

# Built from target/checkout, not the working tree: the DMG names come from
# ${project.version}, so building here would produce <version>-SNAPSHOT DMGs
# instead of release ones.
build_dir="$PROJECT_ROOT/target/checkout"

info "  cd target/checkout && mvn ${MVN_ARGS[*]}"
info ""
version="$(version_from_checkout 2>/dev/null || true)"
if [ -n "$version" ]; then
  info "Building DMGs for ${C_BOLD}$(base_version_for "$version")${C_RESET}"
  info ""
fi

if [ "${1:-}" = "--dry-run" ]; then
  info "${C_DIM}dry run - nothing executed${C_RESET}"
  exit 0
fi

[ "$(uname -s)" = "Darwin" ] || die "Notarization requires macOS."
require_tool mvn
[ -d "$build_dir" ] || die "\
No target/checkout - run the Maven release first (release.sh maven).
The DMGs are built from the checked-out release tag, not the working tree."

cd "$build_dir"
mvn "${MVN_ARGS[@]}"
