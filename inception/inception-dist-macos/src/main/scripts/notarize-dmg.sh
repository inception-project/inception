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

# Submit a signed DMG to Apple's notary service, staple the resulting ticket,
# and verify that Gatekeeper will accept it. The DMG (and the .app inside)
# must already be codesigned with a Developer ID Application identity using
# a secure timestamp and the hardened runtime on the executables.
#
# Args (positional):
#   1: DMG path (or a directory; all *.dmg inside are processed)
#   2: notarytool keychain profile name (see `xcrun notarytool store-credentials`)

set -eu

TARGET="$1"
KEYCHAIN_PROFILE="$2"

case "$KEYCHAIN_PROFILE" in
  ''|'${'*'}')
    echo "ERROR: No valid 'mac.notarization.keychain.profile' configured."
    echo "Create one once with: xcrun notarytool store-credentials <profile-name> --apple-id <id> --team-id <team> --password <app-specific-password>"
    echo "Then pass it via -Dmac.notarization.keychain.profile=<profile-name>."
    exit 1
    ;;
esac

if [ ! -e "$TARGET" ]; then
  echo "WARNING: $TARGET does not exist. Skipping notarization."
  exit 0
fi

if [ -d "$TARGET" ]; then
  set +e
  ls "$TARGET"/*.dmg >/dev/null 2>&1
  no_dmgs=$?
  set -e
  if [ "$no_dmgs" -ne 0 ]; then
    echo "WARNING: No DMG files found in $TARGET. Skipping notarization."
    exit 0
  fi
  DMGS=$(ls "$TARGET"/*.dmg)
else
  DMGS="$TARGET"
fi

for dmg in $DMGS; do
  echo "==> Notarizing $dmg"
  xcrun notarytool submit --keychain-profile "$KEYCHAIN_PROFILE" --wait "$dmg"

  echo "==> Stapling $dmg"
  xcrun stapler staple "$dmg"

  echo "==> Validating staple on $dmg"
  xcrun stapler validate "$dmg"

  echo "==> Gatekeeper assessment for $dmg"
  spctl --assess -t install -vv "$dmg"
done

echo "Notarization complete."
