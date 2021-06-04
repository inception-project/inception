#/bin/sh

# Formatter settings
JDT_CORE_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.core.prefs"

# Save actions
JDT_UI_PREFS="inception-build/src/main/resources/inception/eclipse/org.eclipse.jdt.ui.prefs"

function installPrefs {
  mkdir -p $1/.settings/
  cp -v $JDT_CORE_PREFS $1/.settings/
  cp -v $JDT_UI_PREFS $1/.settings/
}

installPrefs inception-active-learning
installPrefs inception-agreement
installPrefs inception-api
installPrefs inception-api-annotation
installPrefs inception-api-dao
installPrefs inception-api-formats
installPrefs inception-app-webapp
installPrefs inception-boot-loader
installPrefs inception-brat-editor
installPrefs inception-build
installPrefs inception-concept-linking
installPrefs inception-constraints
installPrefs inception-curation
installPrefs inception-curation-legacy
installPrefs inception-diag
installPrefs inception-doc
installPrefs inception-docker
installPrefs inception-example-imls-data-majority
installPrefs inception-export
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
installPrefs inception-io-conll
installPrefs inception-io-json
installPrefs inception-io-tcf
installPrefs inception-io-text
installPrefs inception-io-webanno-tsv
installPrefs inception-io-xmi
installPrefs inception-kb
installPrefs inception-layer-docmetadata
installPrefs inception-log
installPrefs inception-model
installPrefs inception-model-export
installPrefs inception-pdf-editor
installPrefs inception-plugin-api
installPrefs inception-plugin-manager
installPrefs inception-plugin-parent
installPrefs inception-preferences
installPrefs inception-project
installPrefs inception-recommendation
installPrefs inception-recommendation-api
installPrefs inception-remote
installPrefs inception-review-editor
installPrefs inception-scheduling
installPrefs inception-search-core
installPrefs inception-search-mtas
installPrefs inception-security
installPrefs inception-sharing
installPrefs inception-support
installPrefs inception-support-standalone
installPrefs inception-telemetry
installPrefs inception-testing
installPrefs inception-ui-agreement
installPrefs inception-ui-annotation
installPrefs inception-ui-core
installPrefs inception-ui-curation
installPrefs inception-ui-dashboard
installPrefs inception-ui-external-search
installPrefs inception-ui-kb
installPrefs inception-ui-monitoring
installPrefs inception-ui-project
installPrefs inception-ui-search
installPrefs inception-ui-tagsets
installPrefs inception-versioning
installPrefs inception-websocket
installPrefs inception-workload
installPrefs inception-workload-dynamic
installPrefs inception-workload-matrix
