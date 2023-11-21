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

let annoPage: PDFAnnoPage
let annotationContainer: AnnotationContainer

export function initPdfAnno () {
  window.globalEvent = new EventEmitter()
  window.globalEvent.setMaxListeners(0)

  // Create an annocation container.
  annotationContainer = new AnnotationContainer()
  annoPage = new PDFAnnoPage(annotationContainer)

  // FIXME: These should be removed
  window.annotationContainer = annotationContainer
  window.annoPage = annoPage

  // Enable a view mode.
  UI.enableViewMode()

  window.PDFViewerApplication.initializedPromise.then(function () {
    // The event called at page rendered by pdfjs.
    window.PDFViewerApplication.eventBus.on('pagerendered', ev => onPageRendered(ev))
    // Adapt to scale change.
    window.PDFViewerApplication.eventBus.on('scalechanged', ev => onScaleChange(ev))
    window.PDFViewerApplication.eventBus.on('zoomin', ev => onScaleChange(ev))
    window.PDFViewerApplication.eventBus.on('zoomout', ev => onScaleChange(ev))
    window.PDFViewerApplication.eventBus.on('zoomreset', ev => onScaleChange(ev))
    window.PDFViewerApplication.eventBus.on('sidebarviewchanged', ev => onScaleChange(ev))
    window.PDFViewerApplication.eventBus.on('resize', ev => onScaleChange(ev))
  })

  // Init viewer.
  annoPage.initializeViewer(null)

  // Start application.
  annoPage.startViewerApplication()

  installSpanSelection()
  installRelationSelection()

  // Show a content.
  displayViewer()
}

function onPageRendered (ev) {
  console.log('pagerendered:', ev.pageNumber)

  // No action, if the viewer is closed.
  if (!window.PDFViewerApplication.pdfViewer.getPageView(0)) {
    return
  }

  adjustPageGaps()
  removeAnnoLayer()
  renderAnno()
}

function onScaleChange (ev) {
  console.log('scalechanged')
  adjustPageGaps()
  removeAnnoLayer()
  renderAnno()
}

function adjustPageGaps () {
  // Issue Fix.
  // Correctly rendering when changing scaling.
  // The margin between pages is fixed(9px), and never be scaled in default,
  // then manually have to change the margin.
  const scale = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
  document.querySelectorAll('.page').forEach(e => {
    const page = e as HTMLElement
    const borderWidth = `${9 * scale}px`
    page.style.borderTopWidth = borderWidth
    page.style.borderBottomWidth = borderWidth
    page.style.marginBottom = `${-9 * scale}px`
    page.style.marginTop = `${1 * scale}px`
  })
}

/*
 * Remove the annotation layer and the temporary rendering layer.
 */
function removeAnnoLayer () {
  // TODO Remove #annoLayer.
  document.getElementById(svgLayerId)?.remove()
  document.getElementById(annoLayer2Id)?.remove()
}

/*
 * Render annotations saved in the storage.
 */
function renderAnno () {
  // No action, if the viewer is closed.
  if (!window.PDFViewerApplication.pdfViewer.getPageView(0)) {
    return
  }

  // Check already exists.
  if (document.getElementById(svgLayerId) && document.getElementById(annoLayer2Id)) {
    return
  }

  const viewer = document.getElementById('viewer')
  const pageElement = document.querySelector('.page')
  let leftMargin = (viewer.clientWidth - pageElement.clientWidth) / 2

  // At window.width < page.width.
  if (leftMargin < 0) {
    leftMargin = 9
  }

  const height = viewer.clientHeight
  const width = pageElement.clientWidth

  // TODO no need ?
  // Add an annotation layer.
  const annoLayer = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  annoLayer.classList.add('annoLayer')
  annoLayer.id = svgLayerId
  annoLayer.style.position = 'absolute'
  annoLayer.style.top = '0px'
  annoLayer.style.left = `${leftMargin}px`
  annoLayer.style.width = `${width}px`
  annoLayer.style.height = `${height}px`
  annoLayer.style.visibility = 'hidden'
  annoLayer.style.zIndex = '2'

  const annoLayer2 = document.createElement('div')
  annoLayer2.id = annoLayer2Id
  annoLayer2.classList.add('annoLayer')
  annoLayer2.style.position = 'absolute'
  annoLayer2.style.top = '0px'
  annoLayer2.style.left = `${leftMargin}px`
  annoLayer2.style.width = `${width}px`
  annoLayer2.style.height = `${height}px`
  annoLayer2.style.visibility = 'hidden'
  annoLayer2.style.zIndex = '2'

  viewer.style.position = 'relative'
  viewer.appendChild(annoLayer)
  viewer.appendChild(annoLayer2)

  dispatchWindowEvent('annotationlayercreated')

  renderAnnotations()
}

/**
 * Render all annotations.
 */
function renderAnnotations () {
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

export function getAnnotations () {
  const data = {
    action: 'getAnnotations',
    page: window.pageRender
  }
  parent.Wicket.Ajax.ajax({
    m: 'POST',
    ep: data,
    u: window.apiUrl,
    sh: [],
    fh: [function () {
      alert('Something went wrong on requesting annotations from inception backend.')
    }]
  })
}

async function displayViewer () {
  // Display a PDF specified via URL query parameter.
  const q = urijs(document.URL).query(true)
  const pdfURL = q.pdf
  const pdftxtURL = q.pdftxt
  window.apiUrl = q.api

  // Load a PDF file.
  Promise.all([
    annoPage.loadPdftxt(pdftxtURL),
    annoPage.displayViewer(getPDFName(pdfURL), pdfURL)
  ])
    .then(([analyzeResult]) => {
      try {
        // Init textLayers.
        textLayer.setup(analyzeResult)

        window.pagechangeEventCounter = 0
        window.pageRender = 1
        const initAnnotations = function (e) {
          try {
            getAnnotations()
          } finally {
            window.PDFViewerApplication.eventBus.off('pagerendered', initAnnotations)
          }
        }
        window.PDFViewerApplication.eventBus.on('pagerendered', initAnnotations)
        window.PDFViewerApplication.eventBus.on('pagechanging', function (e) {
          pagechangeEventCounter++
          if (e.pageNumber !== window.pageRender) {
            const snapshot = window.pagechangeEventCounter
            const renderTimeout = 500
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
        console.error('Failed to analyze the PDF.', err)

        // Init viewer.
        annoPage.initializeViewer(null)
        // Start application.
        annoPage.startViewerApplication()
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
