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
# limitations under the License.set -e

MVN=${MVN:-'mvn'}
# Do not change the order without a good reason - modules that imported by other modules must come before them!
TS_MODULES="
./inception-support-bootstrap/src/main/ts
./inception-js-api/src/main/ts
./inception-diam/src/main/ts
./inception-io-tei/src/main/ts
./inception-io-xml/src/main/ts
./inception-brat-editor/src/main/ts
./inception-diam-editor/src/main/ts
./inception-external-editor/src/main/ts
./inception-html-apache-annotator-editor/src/main/ts
./inception-html-recogito-editor/src/main/ts
./inception-pdf-editor2/src/main/ts
./inception-project-export/src/main/ts
./inception-recommendation/src/main/ts
./inception-ui-dashboard-activity/src/main/ts
./inception-ui-kb/src/main/ts
./inception-ui-search/src/main/ts
./inception-ui-scheduling/src/main/ts
./inception-assistant/src/main/ts
"

# Store the base directory
BASE_DIR=$(pwd)
REPORT_FILE="$BASE_DIR/outdated-packages-report.txt"
SUMMARY_FILE="$BASE_DIR/outdated-packages-summary.txt"

# Clear previous reports
> "$REPORT_FILE"
> "$SUMMARY_FILE"

echo "======================================"
echo "Checking for outdated npm packages..."
echo "======================================"
echo ""

TOTAL_MODULES=0
MODULES_WITH_OUTDATED=0

for module in $TS_MODULES ; do
  TOTAL_MODULES=$((TOTAL_MODULES + 1))
  
  if [ ! -d "$module" ]; then
    echo "Skipping $module (directory not found)"
    continue
  fi
  
  pushd "$module" > /dev/null
  
  if [ ! -f "package.json" ]; then
    echo "Skipping $module (no package.json found)"
    popd > /dev/null
    continue
  fi
  
  MODULE_NAME=$(pwd)
  echo "Checking: $MODULE_NAME"
  echo "========================================" >> "$REPORT_FILE"
  echo "Module: $MODULE_NAME" >> "$REPORT_FILE"
  echo "========================================" >> "$REPORT_FILE"
  
  # Run npm outdated and capture output
  # npm outdated returns exit code 1 if there are outdated packages, so we need to handle that
  if npm outdated --long 2>&1 | tee -a "$REPORT_FILE" | grep -q .; then
    MODULES_WITH_OUTDATED=$((MODULES_WITH_OUTDATED + 1))
    echo "⚠️  Found outdated packages in $module"
  else
    echo "✓ All packages up to date in $module"
    echo "All packages are up to date." >> "$REPORT_FILE"
  fi
  
  echo "" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  popd > /dev/null
done

# Generate summary
echo "======================================"
echo "Summary"
echo "======================================"
echo "Total modules checked: $TOTAL_MODULES"
echo "Modules with outdated packages: $MODULES_WITH_OUTDATED"
echo ""
echo "Detailed report written to: $REPORT_FILE"

# Write summary to file
{
  echo "======================================"
  echo "Outdated Packages Summary"
  echo "Generated: $(date)"
  echo "======================================"
  echo ""
  echo "Total modules checked: $TOTAL_MODULES"
  echo "Modules with outdated packages: $MODULES_WITH_OUTDATED"
  echo ""
  echo "See $REPORT_FILE for detailed information."
} > "$SUMMARY_FILE"

echo "Summary written to: $SUMMARY_FILE"
