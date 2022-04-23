import './pdfanno.css'
import $ from 'jquery'
import urijs from 'urijs'
import * as textLayer from './page/textLayer'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import { dispatchWindowEvent } from './shared/util'
import EventEmitter from 'events'
import UI from './core/src/UI'
import AnnotationContainer from './core/src/annotation/container'
import AbstractAnnotation from './core/src/annotation/abstract'

let annoPage: PDFAnnoPage;
let annotationContainer: AnnotationContainer;

export function initPdfAnno() {
  window.globalEvent = new EventEmitter()
  window.globalEvent.setMaxListeners(0)

  // Create an annocation container.
  annotationContainer = new AnnotationContainer();
  annoPage = new PDFAnnoPage(annotationContainer);

  // FIXME: These should be removed
  window.annotationContainer = annotationContainer;
  window.annoPage = annoPage;

  // Enable a view mode.
  UI.enableViewMode()

  // The event called at page rendered by pdfjs.
  window.addEventListener('pagerendered', ev => onPageRendered(ev))
  // Adapt to scale change.
  window.addEventListener('scalechange', ev => onScaleChange(ev))

  /**
   *  The entry point.
   */
  window.addEventListener('DOMContentLoaded', async (ev) => onDomContentLoaded(ev))
}

initPdfAnno()

function onDomContentLoaded(ev: Event) {
  // Show loading.
  showLoader(true)

  // Init viewer.
  annoPage.initializeViewer(null)

  // Start application.
  annoPage.startViewerApplication()

  // Show a content.
  displayViewer()
}

function onPageRendered(ev) {
  console.log('pagerendered:', ev.detail.pageNumber)

  // No action, if the viewer is closed.
  if (!window.PDFViewerApplication.pdfViewer.getPageView(0)) {
    return
  }

  adjustPageGaps()
  renderAnno()
}

function onScaleChange(ev) {
  console.log('scalechange')
  adjustPageGaps()
  removeAnnoLayer()
  renderAnno()
}

function adjustPageGaps() {
  // Issue Fix.
  // Correctly rendering when changing scaling.
  // The margin between pages is fixed(9px), and never be scaled in default,
  // then manually have to change the margin.
  let scale = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
  let borderWidth = `${9 * scale}px`
  let marginBottom = `${-9 * scale}px`
  let marginTop = `${1 * scale}px`
  $('.page').css({
    'border-top-width': borderWidth,
    'border-bottom-width': borderWidth,
    marginBottom,
    marginTop
  })
}

/*
 * Remove the annotation layer and the temporary rendering layer.
 */
function removeAnnoLayer() {
  // TODO Remove #annoLayer.
  document.getElementById("annoLayer")?.remove()
  document.getElementById("annoLayer2")?.remove()
}

/*
 * Render annotations saved in the storage.
 */
function renderAnno() {

  // No action, if the viewer is closed.
  if (!window.PDFViewerApplication.pdfViewer.getPageView(0)) {
    return
  }

  // TODO make it a global const.
  const svgLayerId = 'annoLayer'
  const annoLayer2Id = 'annoLayer2'

  // Check already exists.
  if (document.getElementById(svgLayerId) && document.getElementById(annoLayer2Id)) {
    return
  }

  let leftMargin = ($('#viewer').width() - $('.page').width()) / 2

  // At window.width < page.width.
  if (leftMargin < 0) {
    leftMargin = 9
  }

  let height = $('#viewer').height()
  let width = $('.page').width()

  // TODO no need ?
  // Add an annotation layer.
  let $annoLayer = $(`<svg id="${svgLayerId}" class="${svgLayerId}"/>`).css({   // TODO CSSClass.
    position: 'absolute',
    top: '0px',
    left: `${leftMargin}px`,
    width: `${width}px`,
    height: `${height}px`,
    visibility: 'hidden',
    'z-index': 2
  })
  // Add an annotation layer.
  let $annoLayer2 = $(`<div id="${annoLayer2Id}"/>`).addClass('annoLayer').css({   // TODO CSSClass.
    position: 'absolute',
    top: '0px',
    left: `${leftMargin}px`,
    width: `${width}px`,
    height: `${height}px`,
    visibility: 'hidden',
    'z-index': 2
  })

  $('#viewer').css({
    position: 'relative'  // TODO css.
  }).append($annoLayer).append($annoLayer2)

  dispatchWindowEvent('annotationlayercreated')

  renderAnnotations()
}

/**
 * Render all annotations.
 */
function renderAnnotations() {
  const annotations = annotationContainer.getAllAnnotations()
  if (annotations.length === 0) {
    return
  }
  annotations.forEach((a: AbstractAnnotation) => {
    a.render()
    a.enableViewMode()
  })
  dispatchWindowEvent('annotationrendered')
}

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

async function displayViewer() {

  // Display a PDF specified via URL query parameter.
  const q = urijs(document.URL).query(true)
  const pdfURL = q.pdf
  const pdftxtURL = q.pdftxt
  window.apiUrl = q.api

  // Load a PDF file.
  try {
    let { pdf, analyzeResult } = await annoPage.loadPDFFromServer(pdfURL, pdftxtURL)

    setTimeout(() => {
      annoPage.displayViewer(getPDFName(pdfURL), pdf)
    }, 500)

    const listenPageRendered = async () => {
      showLoader(false)
      window.removeEventListener('pagerendered', listenPageRendered)
    }
    window.addEventListener('pagerendered', listenPageRendered)

    // Init textLayers.
    textLayer.setup(analyzeResult)
    annoPage.pdftxt = analyzeResult

    const renderTimeout = 500
    window.pagechangeEventCounter = 0
    window.pageRender = 1;
    let initAnnotations = function (e) {
      try {
        getAnnotations()
      } finally {
        document.removeEventListener('pagerendered', initAnnotations)
      }
    }
    document.addEventListener('pagerendered', initAnnotations)
    document.addEventListener('pagechange', function (e) {
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
    console.error('Failed to analyze the PDF.', err);

    // Init viewer.
    annoPage.initializeViewer(null)
    // Start application.
    annoPage.startViewerApplication()
  }
}

/**
 * Get a PDF name from URL.
 */
function getPDFName(url) {
  const a = url.split('/')
  return a[a.length - 1]
}

/**
 * Show or hide a loding.
 */
function showLoader(display: boolean) {
  if (display) {
    $('#pdfLoading').removeClass('close hidden')
  } else {
    $('#pdfLoading').addClass('close')
    setTimeout(function () {
      $('#pdfLoading').addClass('hidden')
    }, 1000)
  }
}
