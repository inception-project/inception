import './pdfanno.css'
import urijs from 'urijs'
import * as textLayer from './page/textLayer'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import { dispatchWindowEvent } from './shared/util'
import EventEmitter from 'events'
import UI from './core/src/UI'
import AnnotationContainer from './core/src/annotation/container'
import AbstractAnnotation from './core/src/annotation/abstract'
import { installSpanSelection } from './core/src/UI/span'
import { installRelationSelection } from './core/src/UI/relation'

  // TODO make it a global const.
  const svgLayerId = 'annoLayer'
  const annoLayer2Id = 'annoLayer2'

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

  // Show loading.
  showLoader(true)

  // Init viewer.
  annoPage.initializeViewer(null)

  // Start application.
  annoPage.startViewerApplication()

  installSpanSelection()
  installRelationSelection()

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
  document.querySelectorAll('.page').forEach(e => {
    const page = e as HTMLElement;
    let borderWidth = `${9 * scale}px`
    page.style.borderTopWidth = borderWidth
    page.style.borderBottomWidth = borderWidth
    page.style.marginBottom = `${-9 * scale}px`
    page.style.marginTop = `${1 * scale}px`
  })
}

/*
 * Remove the annotation layer and the temporary rendering layer.
 */
function removeAnnoLayer() {
  // TODO Remove #annoLayer.
  document.getElementById(svgLayerId)?.remove()
  document.getElementById(annoLayer2Id)?.remove()
}

/*
 * Render annotations saved in the storage.
 */
function renderAnno() {

  // No action, if the viewer is closed.
  if (!window.PDFViewerApplication.pdfViewer.getPageView(0)) {
    return
  }

  // Check already exists.
  if (document.getElementById(svgLayerId) && document.getElementById(annoLayer2Id)) {
    return
  }

  const viewer = document.getElementById('viewer');
  const pageElement = document.querySelector('.page')
  let leftMargin = (viewer.clientWidth - pageElement.clientWidth) / 2

  // At window.width < page.width.
  if (leftMargin < 0) {
    leftMargin = 9
  }

  let height = viewer.clientHeight
  let width = pageElement.clientWidth

  // TODO no need ?
  // Add an annotation layer.
  let annoLayer = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  annoLayer.classList.add('annoLayer')
  annoLayer.id = svgLayerId
  annoLayer.style.position= 'absolute'
  annoLayer.style.top= '0px'
  annoLayer.style.left= `${leftMargin}px`
  annoLayer.style.width= `${width}px`
  annoLayer.style.height= `${height}px`
  annoLayer.style.visibility= 'hidden'
  annoLayer.style.zIndex = '2'

  let annoLayer2 = document.createElement('div')
  annoLayer2.id = annoLayer2Id
  annoLayer2.classList.add('annoLayer')
  annoLayer2.style.position= 'absolute'
  annoLayer2.style.top= '0px'
  annoLayer2.style.left= `${leftMargin}px`
  annoLayer2.style.width= `${width}px`
  annoLayer2.style.height= `${height}px`
  annoLayer2.style.visibility= 'hidden'
  annoLayer2.style.zIndex = '2'

  viewer.style.position = 'relative';
  viewer.appendChild(annoLayer)
  viewer.appendChild(annoLayer2)

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

export function getAnnotations() {
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
    document.querySelectorAll('#pdfLoading').forEach(e => e.classList.remove('close', 'hidden'))
  } else {
    document.querySelectorAll('#pdfLoading').forEach(e => e.classList.add('close'))
    setTimeout(function () {
      document.querySelectorAll('#pdfLoading').forEach(e => e.classList.add('hidden'))
    }, 1000)
  }
}
