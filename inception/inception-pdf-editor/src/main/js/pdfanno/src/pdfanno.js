require('file-loader?name=index.html!./index.html')
require('file-loader?name=index-debug.html!./index-debug.html')
require('!style-loader!css-loader!./pdfanno.css')

import urijs from 'urijs'

// UI parts.
import * as annoUI from 'anno-ui'

import { dispatchWindowEvent } from './shared/util'
import * as publicApi from './page/public'
import * as searchUI from './page/search'
import * as textLayer from './page/textLayer'
import { showLoader } from './page/util/display'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import * as pdfextractdownload from './page/pdfextractdownload'
import { readPdftxt } from './page/pdf/loadFiles'

/**
 * Global variable.
 */
window.pdfanno = {}

/**
 * Expose public APIs.
 */
publicApi.expose()

/**
 * Annotation functions for a page.
 */
window.annoPage = new PDFAnnoPage()

/**
 *  The entry point.
 */
window.addEventListener('DOMContentLoaded', async () => {

  // UI.
  setupUI()

  // Setup downloadPDFExtractButton
  pdfextractdownload.setup()

  // Show loading.
  showLoader(true)

  // Init viewer.
  window.annoPage.initializeViewer(null)

  // Start application.
  window.annoPage.startViewerApplication()

  // initial tab.
  const q        = urijs(document.URL).query(true)
  const tabIndex = q.tab && parseInt(q.tab, 10)
  if (tabIndex) {
    $(`.nav-tabs a[href="#tab${tabIndex}"]`).click()
  }

  // Show a content.
  displayViewer()
})

function getAnnotations() {
  var data = {
    "action": "getAnnotations",
    "page": window.pageRender
  }
  parent.Wicket.Ajax.ajax({
    "m": "POST",
    "ep": data,
    "u": window.apiUrl,
    "sh": [],
    "fh": [function () {
      alert('Something went wrong on requesting annotations from inception backend.')
    }]
  });
}

async function displayViewer () {

  // Display a PDF specified via URL query parameter.
  const q        = urijs(document.URL).query(true)
  const pdfURL   = q.pdf
  const pdftxtURL = q.pdftxt
  window.apiUrl = q.api

  // Load a PDF file.
  try {
    let { pdf, analyzeResult } = await window.annoPage.loadPDFFromServer(pdfURL, pdftxtURL)

    setTimeout(() => {
      window.annoPage.displayViewer({
        name    : getPDFName(pdfURL),
        content : pdf
      })
    }, 500)

    const listenPageRendered = async () => {
      showLoader(false)
      window.removeEventListener('pagerendered', listenPageRendered)
    }
    window.addEventListener('pagerendered', listenPageRendered)

    // Set the analyzeResult.
    annoUI.uploadButton.setResult(analyzeResult)

    // Init search function.
    searchUI.setup(analyzeResult)

    // Init textLayers.
    textLayer.setup(analyzeResult)
    window.annoPage.pdftxt = analyzeResult

    const renderTimeout = 500
    window.pagechangeEventCounter = 0
    window.pageRender = 1;
    let initAnnotations = function(e) {
      try {
        getAnnotations()
      } finally {
        document.removeEventListener('pagerendered', initAnnotations)
      }
    }
    document.addEventListener('pagerendered', initAnnotations)
    document.addEventListener('pagechange', function(e) {
      pagechangeEventCounter++
      if (e.pageNumber !== window.pageRender) {
        const snapshot = window.pagechangeEventCounter
        setTimeout(() => {
          if (snapshot === pagechangeEventCounter && e.pageNumber !== window.pageRender) {
            window.pageRender = e.pageNumber
            window.pagechangeEventCounter = 0
            getAnnotations()
          }
        }, renderTimeout)
      }
    })

  } catch (err) {

    // Hide a loading, and show the error message.
    showLoader(false)
    const message = 'Failed to analyze the PDF.<br>Reason: ' + err
    annoUI.ui.alertDialog.show({ message })

    // Init viewer.
    window.annoPage.initializeViewer(null)
    // Start application.
    window.annoPage.startViewerApplication()
  }

}

function setupUI () {
  // resizable.
  annoUI.util.setupResizableColumns()

  // Start event listeners.
  annoUI.event.setup()

  // PDF dropdown.
  annoUI.contentDropdown.setup({
    initialText            : 'PDF File',
    overrideWarningMessage : 'Are you sure to load another PDF ?',
    contentReloadHandler   : fileName => {

      dispatchWindowEvent('willChangeContent')

      // Disable UI.
      $('#searchWord, .js-dict-match-file').attr('disabled', 'disabled')

      // Get the content.
      const content = window.annoPage.getContentFile(fileName)

      // Reset annotations displayed.
      window.annoPage.clearAllAnnotations()

      // Display the PDF on the viewer.
      window.annoPage.displayViewer(content)

      // Read pdftxt file.
      readPdftxt(content.file).then(text => {
        dispatchWindowEvent('didChangeContent')
        searchUI.setup(text)
        textLayer.setup(text)
        window.annoPage.pdftxt = text
      }).catch(err => {
        console.log(err)
        return annoUI.ui.alertDialog.show({ message : err })
      })
    }
  })
}

/**
 * Get a PDF name from URL.
 */
function getPDFName (url) {
  const a = url.split('/')
  return a[a.length - 1]
}

window.getPDFName = getPDFName
