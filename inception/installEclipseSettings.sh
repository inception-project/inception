#/bin/sh

# Formatter settings
export JDT_CORE_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
export JDT_UI_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.ui.prefs"

# JUnit configuration
export JUNIT_PATFORM_PROPERTIES="inception-build/src/main/resources/inception/junit-platform.properties"

function installPrefs {
  echo "Installing preferences into $1"
  mkdir -p $1/.settings/
  mkdir -p $1/src/test/resources/
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
  # cp -v $JUNIT_PATFORM_PROPERTIES $1/src/test/resources/
}

export -f installPrefs

# Find directories containing src/main/java and apply installPrefs
find . -maxdepth 1 -type d -exec test -d '{}/src/main/java' ';' -exec sh -c 'installPrefs "{}"' \;
