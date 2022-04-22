import './pdfanno.css'
import $ from 'jquery'

import urijs from 'urijs'

// UI parts.
import * as annoUI from './anno-ui'

import * as publicApi from './page/public'
import * as textLayer from './page/textLayer'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'

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

  // Show loading.
  showLoader(true)

  // Init viewer.
  window.annoPage.initializeViewer(null)

  // Start application.
  window.annoPage.startViewerApplication()

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
  // Start event listeners.
  annoUI.event.setup()
}

/**
 * Get a PDF name from URL.
 */
function getPDFName (url) {
  const a = url.split('/')
  return a[a.length - 1]
}

/**
 * Show or hide a loding.
 */
 function showLoader (display) {
  if (display) {
    $('#pdfLoading').removeClass('close hidden')
  } else {
    $('#pdfLoading').addClass('close')
    setTimeout(function () {
      $('#pdfLoading').addClass('hidden')
    }, 1000)
  }
}
