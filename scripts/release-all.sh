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

# Runs the release steps in order, stopping at the first failure.
#
# Re-runnable: every step decides for itself whether there is anything to do -
# maven refuses on a dirty tree, macos/docker rebuild in place, windows reuses a
# successful CI run, draft leaves an existing draft alone, upload skips assets
# already attached. So after fixing whatever broke, run this again and it carries
# on rather than starting over.
#
# The release is deliberately left as a draft: review the notes and publish in
# the browser. The remaining manual steps are listed by `release.sh status`.
#
# Usage: release-all.sh [--from <step>] [--dry-run]

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=release-functions.shlib
source "$SCRIPT_DIR/release-functions.shlib"

# maven first (it creates target/checkout that the rest read the version from),
# then the builds, then windows - which waits on CI and is by far the slowest -
# then the release itself. upload comes last so it sees every artifact.
STEPS="maven macos windows docker draft upload"

from=''
dry_run=0
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help|help)
      info "Usage: release-all.sh [--from <step>] [--dry-run]"
      info "Steps: $STEPS"
      exit 0 ;;
    --dry-run) dry_run=1 ;;
    --from)
      shift; [ $# -gt 0 ] || die "--from needs a step name."
      from="$1" ;;
    --from=*) from="${1#*=}" ;;
    *) die "Unexpected argument '$1'." ;;
  esac
  shift
done

if [ -n "$from" ]; then
  case " $STEPS " in
    *" $from "*) ;;
    *) die "Unknown step '$from'. Steps: $STEPS" ;;
  esac
fi

dry_args=()
if [ "$dry_run" -eq 1 ]; then
  dry_args=(--dry-run)
  # Everything after maven reads the version from target/checkout, which only
  # exists once release:perform has run. Without it a dry run can only show the
  # first step, so say that instead of failing four times over.
  if ! version_from_checkout >/dev/null 2>&1; then
    info "${C_YELLOW}No target/checkout yet${C_RESET} - the steps after maven derive the"
    info "version from it, so they cannot be previewed before the release has run."
    info ""
    info "Steps that would run, in order:"
    for step in $STEPS; do info "  $step"; done
    exit 0
  fi
fi

skipping=0
[ -n "$from" ] && skipping=1

for step in $STEPS; do
  if [ "$skipping" -eq 1 ]; then
    if [ "$step" = "$from" ]; then
      skipping=0
    else
      info "${C_DIM}--- skip $step${C_RESET}"
      continue
    fi
  fi

  info ""
  info "${C_BOLD}=== $step ===${C_RESET}"
  info ""

  # Not exec'd and not backgrounded: each step's output goes straight to the
  # terminal, and a failure stops the sequence here so the log ends at the
  # problem rather than scrolling past it.
  if ! "$SCRIPT_DIR/release-$step.sh" "${dry_args[@]}"; then
    info ""
    die "Step '$step' failed. Fix it, then resume with:
  release.sh all --from $step"
  fi
done

info ""
info "${C_BOLD}=== done ===${C_RESET}"
info ""
if [ "$dry_run" -eq 1 ]; then
  info "${C_DIM}dry run - nothing was executed${C_RESET}"
  exit 0
fi
info "The release is still a draft. Review the notes and publish it in the browser."
info "Outstanding work: release.sh status"
