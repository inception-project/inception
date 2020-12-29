#/bin/sh

# Formatter settings
JDT_CORE_PREFS="webanno-build/src/main/resources/webanno/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
JDT_UI_PREFS="webanno-build/src/main/resources/webanno/eclipse/org.eclipse.jdt.ui.prefs"

function installPrefs {
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
}

installPrefs webanno-agreement
installPrefs webanno-api
installPrefs webanno-api-annotation
installPrefs webanno-api-automation
installPrefs webanno-api-codebook
installPrefs webanno-api-dao
installPrefs webanno-api-formats
installPrefs webanno-automation
installPrefs webanno-boot-loader
installPrefs webanno-brat
installPrefs webanno-build
installPrefs webanno-codebook
installPrefs webanno-constraints
installPrefs webanno-curation
installPrefs webanno-diag
installPrefs webanno-dkprocore
installPrefs webanno-doc
installPrefs webanno-export
installPrefs webanno-io-conll
installPrefs webanno-io-csv
installPrefs webanno-io-json
installPrefs webanno-io-tcf
installPrefs webanno-io-tei
installPrefs webanno-io-text
installPrefs webanno-io-tsv
installPrefs webanno-io-xmi
installPrefs webanno-model
installPrefs webanno-model-export
installPrefs webanno-plugin-api
installPrefs webanno-plugin-manager
installPrefs webanno-plugin-parent
installPrefs webanno-project
installPrefs webanno-remote
installPrefs webanno-security
installPrefs webanno-support
installPrefs webanno-support-standalone
installPrefs webanno-telemetry
installPrefs webanno-ui-annotation
installPrefs webanno-ui-automation
installPrefs webanno-ui-core
installPrefs webanno-ui-correction
installPrefs webanno-ui-curation
installPrefs webanno-ui-menu
installPrefs webanno-ui-monitoring
installPrefs webanno-ui-project
installPrefs webanno-ui-tagsets
installPrefs webanno-webapp
