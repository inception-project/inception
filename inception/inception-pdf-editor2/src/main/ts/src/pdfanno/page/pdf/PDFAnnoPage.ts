import { dispatchWindowEvent } from '../../shared/util'
import { convertToExportY, paddingBetweenPages } from '../../shared/coords'
import { adjustViewerSize } from '../util/window'
import AnnotationContainer from '../../core/src/annotation/container'
import SpanAnnotation from '../../core/src/annotation/span'
import RelationAnnotation from '../../core/src/annotation/relation'
import { VID } from '@inception-project/inception-js-api'

/**
 * PDFAnno's Annotation functions for Page produced by .
 */
export default class PDFAnnoPage {
  annotationContainer: AnnotationContainer

  constructor (annotationContainer: AnnotationContainer) {
    this.annotationContainer = annotationContainer
    this.autoBind()
  }

  autoBind () {
    Object.getOwnPropertyNames(this.constructor.prototype)
      .filter(prop => typeof this[prop] === 'function')
      .forEach(method => {
        this[method] = this[method].bind(this)
      })
  }

  /**
   * Start PDFAnno Application.
   */
  startViewerApplication () {
    // Adjust the height of viewer.
    adjustViewerSize()
  }

  async displayViewer (name: string, pdfUrl: string): Promise<void> {
    // Reset settings.
    this.resetPDFViewerSettings()

    console.log(`Loading PDF from ${pdfUrl}`)

    // Load PDF.
    return window.PDFViewerApplication.open(pdfUrl).then(() => {
      // Set the PDF file name.
      window.PDFViewerApplication.url = name
    })
  }

  /**
   * Start the viewer.
   */
  initializeViewer (viewerSelector = '#viewer') {
    window.pdf = null
    window.pdfName = null

    // Reset setting.
    this.resetPDFViewerSettings()
  }

  /**
   * Close the viewer.
   */
  closePDFViewer () {
    if (window.PDFViewerApplication) {
      window.PDFViewerApplication.close()
      $('#numPages', window.document).text('')
      dispatchWindowEvent('didCloseViewer')
    }
  }

  /**
   * Reset the setting of PDFViewer.
   */
  resetPDFViewerSettings () {
    localStorage.removeItem('pdfjs.history')
  }

  /**
   * Scroll window to the annotation.
   */
  scrollToAnnotation (id: VID) {
    const annotation = this.annotationContainer.findById(id)

    if (annotation) {
      // scroll to.
      let pageNumber: number
      let y: number

      if (annotation.type === 'span') {
        const spanAnnotation = annotation as SpanAnnotation
        pageNumber = spanAnnotation.page
        y = spanAnnotation.rectangles[0].y
      }

      if (annotation.type === 'relation') {
        const relationAnnotation = annotation as RelationAnnotation
        const d = convertToExportY(relationAnnotation.y1)
        pageNumber = d.pageNumber
        y = d.y
      }

      const pageHeight = this.getViewerViewport().height
      const scale = this.getViewerViewport().scale
      let _y = (pageHeight + paddingBetweenPages) * (pageNumber - 1) + y * scale
      _y -= 100
      document.getElementById('viewer').parentElement.scrollTop = _y

      // highlight.
      annotation.highlight()
      setTimeout(() => annotation.dehighlight(), 1000)
    }
  }

  /**
   * Get the viewport of the viewer.
   */
  getViewerViewport () {
    return window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  }

  /**
   * Load PDF data from url.
   * @memberof PDFAnnoPage
   */
  loadPdf (url: String): Promise<Uint8Array> {
    // add noise to the query parameters so caching is prevented
    const antiCacheUrl = url + '&time=' + new Date().getTime()
    return fetch(antiCacheUrl, {
      method: 'GET',
      mode: 'cors'
    }).then(response => {
      if (response.ok) {
        return response.arrayBuffer()
      } else {
        throw new Error(`HTTP ${response.status} - ${response.statusText}`)
      }
    }).then(buffer => {
      return new Uint8Array(buffer)
    })
  }

  loadVisualModel (url: string): Promise<string> {
    // add noise to the query parameters so caching is prevented
    const antiCacheUrl = url + '&time=' + new Date().getTime()
    return fetch(antiCacheUrl, {
      method: 'GET',
      mode: 'cors'
    }).then(response => {
      if (response.ok) {
        return response.text()
      } else {
        throw new Error(`HTTP ${response.status} - visual model`)
      }
    })
  }
}
