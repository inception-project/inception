#/bin/sh

# Formatter settings
export JDT_CORE_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
export JDT_UI_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.ui.prefs"

function installPrefs {
  echo "Installing preferences into $1"
  mkdir -p $1/.settings/
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
}

export -f installPrefs

# Find directories containing src/main/java and apply installPrefs
find . -maxdepth 1 -type d -exec test -d '{}/src/main/java' ';' -exec sh -c 'installPrefs "{}"' \;
