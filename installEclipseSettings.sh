#/bin/sh

# Formatter settings
JDT_CORE_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
JDT_UI_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.ui.prefs"

function installPrefs {
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
}

installPrefs inception-active-learning
installPrefs inception-plugin-parent
installPrefs inception-website
installPrefs inception-active-learning
installPrefs inception-app-webapp
installPrefs inception-build
installPrefs inception-concept-linking
installPrefs inception-curation
installPrefs inception-doc
installPrefs inception-example-imls-data-majority
installPrefs inception-external-search-core
installPrefs inception-external-search-elastic
installPrefs inception-external-search-pubannotation
installPrefs inception-external-search-solr
installPrefs inception-html-editor
installPrefs inception-image
installPrefs inception-imls-dl4j
installPrefs inception-imls-external
installPrefs inception-imls-lapps
installPrefs inception-imls-opennlp
installPrefs inception-imls-stringmatch
installPrefs inception-imls-weblicht
installPrefs inception-kb
installPrefs inception-layer-docmetadata
installPrefs inception-log
installPrefs inception-pdf-editor
installPrefs inception-recommendation
installPrefs inception-recommendation-api
installPrefs inception-scheduling
installPrefs inception-search-core
installPrefs inception-search-mtas
installPrefs inception-support
installPrefs inception-testing
installPrefs inception-ui-core
installPrefs inception-ui-external-search
installPrefs inception-ui-kb
installPrefs inception-ui-search
installPrefs inception-workload
installPrefs inception-workload-dynamic
installPrefs inception-workload-matrix