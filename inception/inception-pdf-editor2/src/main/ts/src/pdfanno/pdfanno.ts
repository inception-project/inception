import './pdfanno.css'
import urijs from 'urijs'
import * as textLayer from './page/textLayer'
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import { dispatchWindowEvent } from './shared/util'
import EventEmitter from 'events'
import AnnotationContainer from './core/src/annotation/container'
import AbstractAnnotation from './core/src/annotation/abstract'
import { installSpanSelection } from './core/src/UI/span'
import { installRelationSelection } from './core/src/UI/relation'
import { CompactAnnotatedText, CompactSpan, DiamAjax, Offsets, VID } from '@inception-project/inception-js-api'
import { DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax'
import SpanAnnotation from './core/src/annotation/span'
import { getGlyphAtTextOffset, getGlyphsInRange } from './page/textLayer'
import RelationAnnotation from './core/src/annotation/relation'
import { createRect, mapToDocumentCoordinates, mergeRects } from './core/src/render/renderSpan'
import { transform } from './core/src/render/appendChild'
import { makeMarkerMap } from '@inception-project/inception-js-api/src/model/compact/CompactAnnotatedText'
import { CompactTextMarker } from '@inception-project/inception-js-api/src/model/compact/CompactTextMarker'
import { CompactRelation } from '@inception-project/inception-js-api/src/model/compact/CompactRelation'
import { CompactAnnotationMarker } from '@inception-project/inception-js-api/src/model/compact/CompactAnnotationMarker'
import { Rectangle } from '../vmodel/Rectangle'

// TODO make it a global const.
// const svgLayerId = 'annoLayer'
export const annoLayer2Id = 'annoLayer2'

let annoPage: PDFAnnoPage
let annotationContainer: AnnotationContainer
let diamAjax: DiamAjax
let currentFocusPage: number
let pagechangeEventCounter: number

export async function initPdfAnno (ajax: DiamAjax): Promise<void> {
  globalThis.globalEvent = new EventEmitter()
  globalThis.globalEvent.setMaxListeners(0)
  diamAjax = ajax

  // Create an annocation container.
  annotationContainer = new AnnotationContainer()
  annoPage = new PDFAnnoPage(annotationContainer)

  // FIXME: These should be removed
  globalThis.annotationContainer = annotationContainer

  // Enable a view mode.
  // UI.enableViewMode()

  const initPromise = new Promise<void>((resolve) => {
    console.log('Waiting for the document to load...')
    globalThis.PDFViewerApplication.initializedPromise.then(function () {
      // The event called at page rendered by pdfjs.
      globalThis.PDFViewerApplication.eventBus.on('pagerendered', ev => onPageRendered(ev))
      // Adapt to scale change.
      globalThis.PDFViewerApplication.eventBus.on('scalechanged', ev => onScaleChange(ev))
      globalThis.PDFViewerApplication.eventBus.on('zoomin', ev => onScaleChange(ev))
      globalThis.PDFViewerApplication.eventBus.on('zoomout', ev => onScaleChange(ev))
      globalThis.PDFViewerApplication.eventBus.on('zoomreset', ev => onScaleChange(ev))
      globalThis.PDFViewerApplication.eventBus.on('sidebarviewchanged', ev => onScaleChange(ev))
      globalThis.PDFViewerApplication.eventBus.on('resize', ev => onScaleChange(ev))

      const loadedCallback = () => {
        console.log('Document loaded...')
        globalThis.PDFViewerApplication.eventBus.off('pagerendered', loadedCallback)
        resolve()
      }

      globalThis.PDFViewerApplication.eventBus.on('pagerendered', loadedCallback)
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
  if (!globalThis.PDFViewerApplication.pdfViewer.getPageView(0)) {
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
  const scale = globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
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
  if (!globalThis.PDFViewerApplication.pdfViewer.getPageView(0)) {
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
  if (!viewer) {
    console.error('Cannot render: no viewer element found.')
    return
  }

  const pageElement = document.querySelector('.page')
  if (!pageElement) {
    console.error('Cannot render: no page element found.')
    return
  }

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
  const page = textLayer.findPageForTextOffset(offset)?.index
  if (!page) {
    console.error(`No page found for offset: ${offset}`)
    return
  }

  const rectangles = mapToDocumentCoordinates(getGlyphsInRange([offset, offset + 1]).map(g => g.bbox))
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
  const child = createRect(rectangles[0])
  base.appendChild(child)

  const viewport = globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  base = transform(annoLayer, base, viewport)
  annoLayer.appendChild(base)
  child.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'center' })
  setTimeout(() => base.remove(), 5000) // Need to defer so scrolling can complete
}

export function getAnnotations () {
  const focusPage = textLayer.getPage(currentFocusPage)

  if (!focusPage) {
    console.error(`Cannot find page ${currentFocusPage}`)
    return
  }

  const pageBefore = textLayer.getPageBefore(currentFocusPage)
  const extendedBegin = (pageBefore || focusPage)?.range[0]
  const pageAfter = textLayer.getPageAfter(currentFocusPage)
  const extendedEnd = (pageAfter || focusPage)?.range[1]

  const options : DiamLoadAnnotationsOptions = {
    range: [extendedBegin, extendedEnd],
    includeText: false
  }

  diamAjax.loadAnnotations(options).then((doc: CompactAnnotatedText) => {
    annotationContainer.clear()

    const annotationMarkers = makeMarkerMap(doc.annotationMarkers)

    console.log(`Loaded ${doc.spans?.length || '0'} spans and ${doc.relations?.length || '0'} relations in range [${doc.window}]`)

    if (doc.spans) {
      for (const s of doc.spans) {
        makeSpan(s, doc, annotationMarkers)
      }
    }

    if (doc.relations) {
      for (const r of doc.relations) {
        makeRelation(r, annotationMarkers)
      }
    }

    if (doc.textMarkers) {
      for (const m of doc.textMarkers) {
        makeTextMarker(m, doc)
      }
    }

    renderAnnotations()
  })
}

function makeSpan (s: CompactSpan, doc: CompactAnnotatedText, annotationMarkers: Map<VID, Array<CompactAnnotationMarker>>) {
  const offsets = s[1]
  const begin = offsets[0][0] + doc.window[0]
  const end = offsets[0][1] + doc.window[0]
  const range: Offsets = [begin, end]

  const page = textLayer.findPageForTextOffset(begin)?.index
  if (page === undefined) {
    console.warn(`No page found for span range ${range}`)
    return
  }

  const rectangles = calculateRectangles(range)
  if (!rectangles) {
    return
  }

  const span = new SpanAnnotation()
  span.vid = `${s[0]}`
  span.textRange = range
  span.page = page
  span.color = s[2]?.c || '#FFF'
  span.text = s[2]?.l || ''
  span.rectangles = rectangles
  annotationMarkers.get(s[0])?.forEach(m => span.classList.push(`marker-${m[0]}`))
  span.save()
}

function makeRelation (r: CompactRelation, annotationMarkers: Map<VID, Array<CompactAnnotationMarker>>) {
  const source = annotationContainer.findById(r[1][0][0])
  const target = annotationContainer.findById(r[1][1][0])

  if (!source || !target) {
    console.warn(`Cannot find source or target for relation ${r[0]}`)
    return
  }

  const rel = new RelationAnnotation()
  rel.vid = `${r[0]}`
  rel.rel1Annotation = source as SpanAnnotation
  rel.rel2Annotation = target as SpanAnnotation
  rel.color = r[2]?.c
  rel.text = r[2]?.l || null
  annotationMarkers.get(r[0])?.forEach(m => rel.classList.push(`marker-${m[0]}`))
  rel.save()
}

function makeTextMarker (m: CompactTextMarker, doc: CompactAnnotatedText) {
  const offsets = m[1]
  const begin = offsets[0][0] + doc.window[0]
  const end = offsets[0][1] + doc.window[0]
  const range: Offsets = [begin, end]

  const page = textLayer.findPageForTextOffset(begin)?.index
  if (page === undefined) {
    console.warn(`No page found for marker range ${range}`)
    return
  }

  const rectangles = calculateRectangles(range)
  if (!rectangles) {
    return
  }

  const marker = new SpanAnnotation()
  marker.textRange = range
  marker.page = page
  marker.knob = false
  marker.border = false
  marker.rectangles = rectangles
  marker.classList = [`marker-${m[0]}`]
  marker.save()
}

function calculateRectangles (range: [number, number]): Rectangle[] | null {
  let rectangles : Rectangle[]
  if (range[0] === range[1]) {
    const glyph = getGlyphAtTextOffset(range[0])
    if (!glyph) {
      console.warn(`No glyph found for offset ${range[0]}`)
      return null
    }
    rectangles = [new Rectangle({
      p: glyph.page,
      x: glyph.bbox.x,
      y: glyph.bbox.y,
      w: 3,
      h: glyph.bbox.h
    })]
  } else {
    rectangles = mergeRects(getGlyphsInRange(range))
    if (!rectangles?.length) {
      console.warn(`No glyphs found for range ${range}`)
      return null
    }
  }
  return rectangles
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
        currentFocusPage = 1
        const initAnnotations = function (e) {
          try {
            getAnnotations()
          } finally {
            globalThis.PDFViewerApplication.eventBus.off('pagerendered', initAnnotations)
          }
        }
        globalThis.PDFViewerApplication.eventBus.on('pagerendered', initAnnotations)
        globalThis.PDFViewerApplication.eventBus.on('pagechanging', function (e) {
          pagechangeEventCounter++
          if (e.pageNumber !== currentFocusPage) {
            const snapshot = pagechangeEventCounter
            const renderTimeout = 500
            setTimeout(() => {
              if (snapshot === pagechangeEventCounter && e.pageNumber !== currentFocusPage) {
                currentFocusPage = e.pageNumber
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
