require('file-loader?name=index.html!./index.html')
require('file-loader?name=index-debug.html!./index-debug.html')
require('!style-loader!css-loader!./pdfanno.css')

// /tab=TAB&pdf=PDFURL&anno=ANNOURL&move

import urijs from 'urijs'

// UI parts.
import * as annoUI from 'anno-ui'

import { dispatchWindowEvent, parseUrlQuery } from './shared/util'
import { unlistenWindowLeaveEvent } from './page/util/window'
import * as publicApi from './page/public'
import * as searchUI from './page/search'
import * as textLayer from './page/textLayer'
// import * as pdftxtDownload from './page/pdftxtdownload'
import { showLoader } from './page/util/display'
// import * as ws from './page/socket'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import * as deepscholar from './deepscholar'
import * as constants from './shared/constants'
import * as pdfextractdownload from './page/pdfextractdownload'
import { readPdftxt } from './page/pdf/loadFiles'

// XXX
process.env.SERVER_PATH = '0.4.1'

/**
 * API root point.
 */
if (process.env.NODE_ENV === 'production') {
  window.API_DOMAIN = 'https://pdfanno.hshindo.com'
  window.API_PATH = '/' + process.env.SERVER_PATH + '/'
  window.API_ROOT = window.API_DOMAIN + window.API_PATH
} else {
  window.API_DOMAIN = 'http://localhost:3000'
  window.API_PATH = '/'
  window.API_ROOT = window.API_DOMAIN + window.API_PATH
}

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
 * Get the y position in the annotation.
 */
function _getY (annotation) {

  if (annotation.rectangles) {
    return annotation.rectangles[0].y

  } else if (annotation.y1) {
    return annotation.y1

  } else {
    return annotation.y
  }
}

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

  if (deepscholar.isTarget()) {
    // Display for DeepScholar.
    deepscholar.initialize()

  } else {
    // Show a content.
    displayViewer()
  }

})

// BEGIN INCEpTION EXTENSION - #802 - Rendering annotations pagewise
function getAnnotations() {
  var data = {
    "action": "getAnnotations",
    "page": window.pageNumber
  }
  parent.Wicket.Ajax.ajax({
    "m": "POST",
    "ep": data,
    "u": window.apiUrl,
    "sh": [],
    "fh": [function () {
      console.log('Something went wrong on requesting annotations from inception backend.')
    }]
  });
}
// END INCEpTION EXTENSION

async function displayViewer () {

  // Display a PDF specified via URL query parameter.
  const q        = urijs(document.URL).query(true)
// BEGIN INCEpTION EXTENSION - #593 - Add PDFAnno sources
/*
  const pdfURL   = q.pdf || getDefaultPDFURL()
*/
  const pdfURL   = q.pdf
// END INCEpTION EXTENSION
// BEGIN INCEpTION EXTENSION - #624 - Integration of PDFExtractor
  const pdftxtURL = q.pdftxt
// END INCEpTION EXTENSION
  const annoURL  = q.anno
  const moveTo   = q.move
// BEGIN INCEpTION EXTENSION - #838 - Creation of spans in PDF editor
  window.apiUrl = q.api
// END INCEpTION EXTENSION

  // Load a PDF file.
  try {
// BEGIN INCEpTION EXTENSION - #624 - Integration of PDFExtractor
/*
    let { pdf, analyzeResult } = await window.annoPage.loadPDFFromServer(pdfURL)
*/
    let { pdf, analyzeResult } = await window.annoPage.loadPDFFromServer(pdfURL, pdftxtURL)
// END INCEpTION EXTENSION

    setTimeout(() => {
      window.annoPage.displayViewer({
        name    : getPDFName(pdfURL),
        content : pdf
      })
    }, 500)

    const listenPageRendered = async () => {
      showLoader(false)

      // Load and display annotations, if annoURL is set.
      if (annoURL) {
// BEGIN INCEpTION EXTENSION - #627 - Retrieval of existing annotations in PDF editor frontend
/*
        let anno = await window.annoPage.loadAnnoFileFromServer(annoURL)

        window.annoPage.importAnnotation({
          primary     : true,
          annotations : [anno],
          colorMap    : annoUI.labelInput.getColorMap()
        }, true)

        // Move to the annotation.
        if (moveTo) {
          setTimeout(() => {
            window.annoPage.scrollToAnnotation(moveTo)
          }, 500)
        }
*/
// BEGIN INCEpTION EXTENSION - #802 - Rendering annotations pagewise
/*
        parent.Wicket.Ajax.ajax({
          "m" : "GET",
          "u" : annoURL,
          "sh" : [],
          "fh": [function() {
              console.log('Something went wrong on requesting annotations from inception backend.')
          }]
        });
*/
// END INCEpTION EXTENSION - #802
// END INCEpTION EXTENSION - #627
      }
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

// BEGIN INCEpTION EXTENSION - #802 - Rendering annotations pagewise
    window.pageNumber = 1;
    getAnnotations()

    document.addEventListener('pagechange', function(e) {
      if (e.pageNumber !== e.previousPageNumber) {
        console.log("page change: " + e.previousPageNumber + " to " + e.pageNumber)
        window.pageNumber = e.pageNumber
        setTimeout(() => {
          if (window.pageNumber === e.pageNumber) {
            console.log("no further page changes after 1 second, requesting annotations ...")
            getAnnotations()
          }
        }, 1500)
      }
    })
// END INCEpTION EXTENSION

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

  // Browse button.
  annoUI.browseButton.setup({
    loadFiles                          : window.annoPage.loadFiles,
    clearAllAnnotations                : window.annoPage.clearAllAnnotations,
    displayCurrentReferenceAnnotations : () => window.annoPage.displayAnnotation(false, false),
    displayCurrentPrimaryAnnotations   : () => window.annoPage.displayAnnotation(true, false),
    getContentFiles                    : () => window.annoPage.contentFiles,
    getAnnoFiles                       : () => window.annoPage.annoFiles,
    closePDFViewer                     : window.annoPage.closePDFViewer
  })

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

  // Primary anno dropdown.
  annoUI.primaryAnnoDropdown.setup({
    clearPrimaryAnnotations  : window.annoPage.clearAllAnnotations,
    displayPrimaryAnnotation : annoName => window.annoPage.displayAnnotation(true)
  })

  // Reference anno dropdown.
  annoUI.referenceAnnoDropdown.setup({
    displayReferenceAnnotations : annoNames => window.annoPage.displayAnnotation(false)
  })

  // Anno list dropdown.
  annoUI.annoListDropdown.setup({
    getAnnotations : () => {

      // Get displayed annotations.
      let annotations = window.annoPage.getAllAnnotations()

      // Filter only Primary.
      annotations = annotations.filter(a => {
        return !a.readOnly
      })

      // Sort by offsetY.
      annotations = annotations.sort((a1, a2) => {
        return _getY(a1) - _getY(a2)
      })

      return annotations
    },
    scrollToAnnotation : window.annoPage.scrollToAnnotation
  })

  // Download anno button.
  annoUI.downloadButton.setup({
    getAnnotationTOMLString : window.annoPage.exportData,
    getDownloadFileName     : () => {
      if (parseUrlQuery()['paper_id']) {
        // return parseUrlQuery()['paper_id'] + '.pdf'
        return parseUrlQuery()['paper_id'] + '.' + constants.ANNO_FILE_EXTENSION
      }
      if (window.annoPage.getCurrentContentFile() && window.annoPage.getCurrentContentFile().name) {
        let filename = window.annoPage.getCurrentContentFile().name
        return filename.replace(/\.pdf$/i, '.' + constants.ANNO_FILE_EXTENSION)
      } else {
        return 'noname.' + constants.ANNO_FILE_EXTENSION
      }
    },
    didDownloadCallback : unlistenWindowLeaveEvent
  })

  // Download pdftxt button.
  // pdftxtDownload.setup()

  // Label input.
  annoUI.labelInput.setup({
    getSelectedAnnotations : window.annoPage.getSelectedAnnotations,
    saveAnnotationText     : (id, text) => {
      const annotation = window.annoPage.findAnnotationById(id)
      if (annotation) {
        annotation.text = text
        annotation.save()
        annotation.enableViewMode()
        dispatchWindowEvent('annotationUpdated')
      }
    },
    createSpanAnnotation : window.annoPage.createSpan,
    createRelAnnotation  : window.annoPage.createRelation,
    colorChangeListener  : v => {
      window.iframeWindow.annotationContainer.changeColor(v)
    }
  })

  // Upload button.
  annoUI.uploadButton.setup({
    getCurrentDisplayContentFile : () => {
      return window.annoPage.getCurrentContentFile()
    },
    uploadFinishCallback : (resultText) => {
      searchUI.setup(resultText)
      textLayer.setup(resultText)
      window.annoPage.pdftxt = resultText
    }
  })

}

/**
 * Get the URL of the default PDF.
 */
function getDefaultPDFURL () {
  // e.g. https://paperai.github.io:80/pdfanno/pdfs/P12-1046.pdf
  const pathnames = location.pathname.split('/')
  const pdfURL = location.protocol + '//' + location.hostname + ':' + location.port + pathnames.slice(0, pathnames.length - 1).join('/') + '/pdfs/P12-1046.pdf'
  // console.log(location.pathname, pathnames, pdfURL)
  return pdfURL
}

/**
 * Get a PDF name from URL.
 */
function getPDFName (url) {
  const a = url.split('/')
  return a[a.length - 1]
}
window.getPDFName = getPDFName

// WebSocket.
// ws.setup()
