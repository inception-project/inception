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

installPrefsinception-active-learning
installPrefs inception-agreement
installPrefs inception-annotation-storage
installPrefs inception-api
installPrefs inception-api-annotation
installPrefs inception-api-dao
installPrefs inception-api-editor
installPrefs inception-api-formats
installPrefs inception-api-render
installPrefs inception-api-schema
installPrefs inception-app-webapp
installPrefs inception-app.iml
installPrefs inception-boot-loader
installPrefs inception-bootstrap
installPrefs inception-brat-editor
installPrefs inception-build
installPrefs inception-concept-linking
installPrefs inception-constraints
installPrefs inception-curation
installPrefs inception-curation-legacy
installPrefs inception-diag
installPrefs inception-diam
installPrefs inception-diam-editor
installPrefs inception-diam-word-alignment-editor
installPrefs inception-doc
installPrefs inception-docker
installPrefs inception-documents
installPrefs inception-editor-api
installPrefs inception-example-imls-data-majority
installPrefs inception-experimental-editor
installPrefs inception-export
installPrefs inception-external-editor
installPrefs inception-external-search-core
installPrefs inception-external-search-elastic
installPrefs inception-external-search-mtas-embedded
installPrefs inception-external-search-pubannotation
installPrefs inception-external-search-solr
installPrefs inception-guidelines
installPrefs inception-html-editor
installPrefs inception-html-recogito-editor
installPrefs inception-image
installPrefs inception-imls-dl4j
installPrefs inception-imls-elg
installPrefs inception-imls-external
installPrefs inception-imls-hf
installPrefs inception-imls-lapps
installPrefs inception-imls-opennlp
installPrefs inception-imls-stringmatch
installPrefs inception-imls-weblicht
installPrefs inception-io-conll
installPrefs inception-io-html
installPrefs inception-io-imscwb
installPrefs inception-io-intertext
installPrefs inception-io-json
installPrefs inception-io-lif
installPrefs inception-io-nif
installPrefs inception-io-perseus
installPrefs inception-io-tcf
installPrefs inception-io-tei
installPrefs inception-io-text
installPrefs inception-io-webanno-tsv
installPrefs inception-io-xmi
installPrefs inception-io-xml
installPrefs inception-js-api
installPrefs inception-kb
installPrefs inception-kb-fact-linking
installPrefs inception-layer-docmetadata
installPrefs inception-log
installPrefs inception-model
installPrefs inception-model-export
installPrefs inception-pdf-editor
installPrefs inception-pdf-editor2
installPrefs inception-plugin-api
installPrefs inception-plugin-manager
installPrefs inception-plugin-parent
installPrefs inception-preferences
installPrefs inception-project
installPrefs inception-project-export
installPrefs inception-project-initializers
installPrefs inception-project-initializers-basic
installPrefs inception-project-initializers-doclabeling
installPrefs inception-project-initializers-sentencelabeling
installPrefs inception-project-initializers-wikidatalinking
installPrefs inception-recommendation
installPrefs inception-recommendation-api
installPrefs inception-remote
installPrefs inception-review-editor
installPrefs inception-scheduling
installPrefs inception-schema
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
installPrefs inception-ui-dashboard-activity
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
installPrefs inception-workload-ui
