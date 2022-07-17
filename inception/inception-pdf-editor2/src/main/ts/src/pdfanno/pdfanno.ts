import './pdfanno.css'
import urijs from 'urijs'
import * as textLayer from './page/textLayer'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import { dispatchWindowEvent } from './shared/util'
import EventEmitter from 'events'
import UI from './core/src/UI'
import AnnotationContainer from './core/src/annotation/container'
import AbstractAnnotation from './core/src/annotation/abstract'
import { installSpanSelection, mergeRects } from './core/src/UI/span'
import { installRelationSelection } from './core/src/UI/relation'
import { CompactAnnotatedText, DiamAjax } from '@inception-project/inception-js-api'
import { DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax'
import SpanAnnotation from './core/src/annotation/span'
import { getGlyphsInRange } from './page/textLayer'
import RelationAnnotation from './core/src/annotation/relation'
import { createRect, mapToDocumentCoordinates } from './core/src/render/renderSpan'
import { transform } from './core/src/render/appendChild'

// TODO make it a global const.
// const svgLayerId = 'annoLayer'
export const annoLayer2Id = 'annoLayer2'

let annoPage: PDFAnnoPage
let annotationContainer: AnnotationContainer
let diamAjax: DiamAjax
let pageRender: number
let pagechangeEventCounter: number

export async function initPdfAnno (ajax: DiamAjax): Promise<void> {
  window.globalEvent = new EventEmitter()
  window.globalEvent.setMaxListeners(0)
  diamAjax = ajax

  // Create an annocation container.
  annotationContainer = new AnnotationContainer()
  annoPage = new PDFAnnoPage(annotationContainer)

  // FIXME: These should be removed
  window.annotationContainer = annotationContainer

  // Enable a view mode.
  UI.enableViewMode()

  const initPromise = new Promise<void>((resolve) => {
    console.log('Waiting for the document to load...')
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

      const loadedCallback = () => {
        console.log('Document loaded...')
        window.PDFViewerApplication.eventBus.off('pagerendered', loadedCallback)
        resolve()
      }

      window.PDFViewerApplication.eventBus.on('pagerendered', loadedCallback)
    })
  })

  // Init viewer.
  annoPage.initializeViewer(undefined)

  // Start application.
  annoPage.startViewerApplication()

  installSpanSelection()
  installRelationSelection()

  // Show a content.
  displayViewer()

  return initPromise
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
  // // TODO Remove #annoLayer.
  // document.getElementById(svgLayerId)?.remove()
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
  if (
  //    document.getElementById(svgLayerId) &&
    document.getElementById(annoLayer2Id)
  ) {
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
  // let annoLayer = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  // annoLayer.classList.add('annoLayer')
  // annoLayer.id = svgLayerId
  // annoLayer.style.position= 'absolute'
  // annoLayer.style.top= '0px'
  // annoLayer.style.left= `${leftMargin}px`
  // annoLayer.style.width= `${width}px`
  // annoLayer.style.height= `${height}px`
  // annoLayer.style.visibility= 'hidden'
  // annoLayer.style.zIndex = '2'

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
  // viewer.appendChild(annoLayer)
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

export function scrollTo (offset: number, position: string): void {
  const page = textLayer.findPageForOffset(offset)?.index
  if (!page) {
    console.error(`No page found for offset: ${offset}`)
    return
  }

  const rectangles = mapToDocumentCoordinates(getGlyphsInRange([offset, offset + 1]).map(g => g.bbox), page)
  const annoLayer = document.getElementById('annoLayer2')
  if (!annoLayer) {
    console.error('No annoLayer found.')
    return
  }

  if (!rectangles?.length) {
    console.warn(`No glyphs found for offset: ${offset} - scrolling only to page`)
    document.querySelector(`.page[data-page-number="${page}"]`)?.scrollIntoView()
    return
  }

  let base: HTMLElement = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = '10'
  const child = createRect(undefined, rectangles[0])
  base.appendChild(child)

  const viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  base = transform(annoLayer, base, viewport)
  annoLayer.appendChild(base)
  child.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'center' })
  setTimeout(() => base.remove(), 5000) // Need to defer so scrolling can complete
}

export function getAnnotations () {
  const options : DiamLoadAnnotationsOptions = {
    range: textLayer.getPage(pageRender).range,
    includeText: false
  }

  diamAjax.loadAnnotations(options).then((doc: CompactAnnotatedText) => {
    annotationContainer.clear()

    if (doc.spans) {
      console.log(`Loaded ${doc.spans.length} span annotations`)
      for (const s of doc.spans) {
        const span = new SpanAnnotation()
        span.vid = `${s[0]}`
        span.textRange = [s[1][0][0] + doc.window[0], s[1][0][1] + doc.window[0]]
        span.page = textLayer.findPageForOffset(span.textRange[0]).index
        span.color = s[2].c
        span.text = s[2].l
        span.rectangles = mergeRects(getGlyphsInRange(span.textRange).map(g => g.bbox))
        span.save()
      }
    }

    if (doc.relations) {
      console.log(`Loaded ${doc.relations.length} relations annotations`)
      for (const r of doc.relations) {
        const rel = new RelationAnnotation()
        rel.vid = `${r[0]}`
        rel.rel1Annotation = annotationContainer.findById(r[1][0][0])
        rel.rel2Annotation = annotationContainer.findById(r[1][1][0])
        rel.color = r[2].c
        rel.text = r[2].l
        rel.save()
      }
    }

    if (doc.textMarkers) {
      for (const m of doc.textMarkers) {
        const span = new SpanAnnotation()
        span.textRange = [m[1][0][0] + doc.window[0], m[1][0][1] + doc.window[0]]
        span.page = textLayer.findPageForOffset(span.textRange[0]).index
        span.color = 'blue'
        span.knob = false
        span.border = false
        span.rectangles = mergeRects(getGlyphsInRange(span.textRange).map(g => g.bbox))
        span.save()
      }
    }

    renderAnnotations()
  })
}

async function displayViewer (): Promise<void> {
  // Display a PDF specified via URL query parameter.
  const q = urijs(document.URL).query(true)
  const pdfUrl = q.pdf
  const vModelUrl = q.vmodel

  // Load a PDF file.
  return Promise.all([
    annoPage.loadVisualModel(vModelUrl),
    annoPage.displayViewer(getPDFName(pdfUrl), pdfUrl)
  ])
    .then(([vModel]) => {
      console.log('Loaded visual model and viewer')

      try {
        // Init textLayers.
        textLayer.setup(vModel)

        pagechangeEventCounter = 0
        pageRender = 1
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
          if (e.pageNumber !== pageRender) {
            const snapshot = pagechangeEventCounter
            const renderTimeout = 500
            setTimeout(() => {
              if (snapshot === pagechangeEventCounter && e.pageNumber !== pageRender) {
                pageRender = e.pageNumber
                pagechangeEventCounter = 0
                getAnnotations()
              }
            }, renderTimeout)
          }
        })
      } catch (err) {
        console.error('Failed to analyze the PDF.', err)

        // Init viewer.
        annoPage.initializeViewer()
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
