#!/usr/bin/env python3
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

# Strips non-Windows native binaries (.dylib, .jnilib, .so) from JARs that
# already contain Windows natives (.dll), to slim down the bundle. No
# codesigning step — Authenticode signing of native DLLs is not currently
# performed by this build.
#
# Implemented in Python (rather than shell) because the Git Bash environment
# on the Windows GitHub runner does not ship a `zip` binary, and Python is
# preinstalled on every runner we target.

import os
import re
import sys
import zipfile

FOREIGN_NATIVE_RE = re.compile(r"\.(dylib|jnilib|so|so\.[0-9.]+)$")


def strip_foreign_natives(jar_path: str) -> None:
    with zipfile.ZipFile(jar_path, "r") as zin:
        names = zin.namelist()

    if not any(name.endswith(".dll") for name in names):
        return

    print(f"--> Found Windows native libraries in: {os.path.basename(jar_path)}")

    to_remove = {name for name in names if FOREIGN_NATIVE_RE.search(name)}
    if not to_remove:
        return

    for name in sorted(to_remove):
        print(f"    Stripping foreign native: {name}")

    tmp_path = jar_path + ".tmp"
    with zipfile.ZipFile(jar_path, "r") as zin, \
         zipfile.ZipFile(tmp_path, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename in to_remove:
                continue
            zout.writestr(item, zin.read(item.filename))
    os.replace(tmp_path, jar_path)


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(f"Usage: {argv[0]} <input-dir>", file=sys.stderr)
        return 2

    input_dir = argv[1]
    if not os.path.isdir(input_dir):
        print(f"Input directory {input_dir} does not exist. Aborting.", file=sys.stderr)
        return 1

    print(f"Hunting for native libraries in {input_dir}...")

    jars = sorted(
        os.path.join(input_dir, name)
        for name in os.listdir(input_dir)
        if name.endswith(".jar")
    )
    if not jars:
        print(f"No JAR files found in {input_dir}. Aborting.", file=sys.stderr)
        return 1

    for jar_path in jars:
        strip_foreign_natives(jar_path)

    print("Native library processing complete.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
