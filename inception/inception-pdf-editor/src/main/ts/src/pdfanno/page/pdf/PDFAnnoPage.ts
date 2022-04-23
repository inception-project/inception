import * as annoUI from '../../anno-ui'
import { anyOf, dispatchWindowEvent } from '../../shared/util'
import { convertToExportY, paddingBetweenPages, nextZIndex } from '../../shared/coords'
import { adjustViewerSize } from '../util/window'
import AnnotationContainer from '../../core/src/annotation/container'
import AbstractAnnotation from '../../core/src/annotation/abstract'

/**
 * PDFAnno's Annotation functions for Page produced by .
 */
export default class PDFAnnoPage {

  annotationContainer: AnnotationContainer;

  constructor(annotationContainer: AnnotationContainer) {
    this.annotationContainer = annotationContainer;
    this.autoBind()
  }

  autoBind() {
    Object.getOwnPropertyNames(this.constructor.prototype)
      .filter(prop => typeof this[prop] === 'function')
      .forEach(method => {
        this[method] = this[method].bind(this)
      })
  }

  /**
   * Start PDFAnno Application.
   */
  startViewerApplication() {
    // Adjust the height of viewer.
    adjustViewerSize()

    dispatchWindowEvent('iframeReady')
  }

  displayViewer(contentFile) {

    // Reset settings.
    this.resetPDFViewerSettings()

    // Load PDF.
    const uint8Array = new Uint8Array(contentFile.content)
    window.PDFViewerApplication.open(uint8Array)

    // Set the PDF file name.
    window.PDFView.url = contentFile.name
  }

  /**
   * Start the viewer.
   */
  initializeViewer(viewerSelector = '#viewer') {

    window.pdf = null
    window.pdfName = null

    // Reset setting.
    this.resetPDFViewerSettings()
  }

  /**
   * Close the viewer.
   */
  closePDFViewer() {
    if (window.PDFViewerApplication) {
      window.PDFViewerApplication.close()
      $('#numPages', window.document).text('')
      // this.currentContentFile = null
      dispatchWindowEvent('didCloseViewer')
    }
  }

  /**
   * Reset the setting of PDFViewer.
   */
  resetPDFViewerSettings() {
    localStorage.removeItem('database')
  }

  /**
   * Find an annotation by id.
   */
  findAnnotationById(id: String): AbstractAnnotation{
    // FIXME: Should access the annotationContainer via "this" here, but currently seems not to be
    // working...
    return window.annotationContainer.findById(id)
  }

  /**
   * Clear the all annotations from the view and storage.
   */
  clearAllAnnotations() {
    if (this.annotationContainer) {
      this.annotationContainer.getAllAnnotations().forEach(a => a.destroy())
    }
  }

  /**
   * Add an annotation to the container.
   */
  addAnnotation(annotation: AbstractAnnotation) {
    this.annotationContainer.add(annotation)
  }

  validateSchemaErrors(errors) {
    let messages = []
    errors.forEach(error => {
      Object.keys(error).forEach(key => {
        let value = error[key]
        value = typeof value === 'object' ? JSON.stringify(value) : value
        messages.push(`${key}: ${value}`)
      })
      messages.push('')
    })
    return messages.join('<br />')
  }

  /**
   * Import annotations from UI.
   */
  importAnnotation(paperData, isPrimary: boolean) {
    // FIXME: Should access the annotationContainer via "this" here, but currently seems not to be
    // working...
    window.annotationContainer.importAnnotations(paperData, isPrimary).then(() => {
      // Notify annotations added.
      dispatchWindowEvent('annotationrendered')
    }).catch(errors => {
      let message = errors
      if (Array.isArray(errors)) {
        message = this.validateSchemaErrors(errors)
      }
      console.error('Unable to import annotations.', errors);
    })
  }

  /**
   * Scroll window to the annotation.
   */
  scrollToAnnotation(id: String) {

    let annotation = this.findAnnotationById(id)

    if (annotation) {

      // scroll to.
      let pageNumber, y
      if (annotation.type === 'span') {
        pageNumber = annotation.page
        y = annotation.rectangles[0].y
      } else {
        let _y = annotation.y || annotation.y1
        let d = convertToExportY(_y)
        pageNumber = d.pageNumber
        y = d.y
      }
      let pageHeight = this.getViewerViewport().height
      let scale = this.getViewerViewport().scale
      let _y = (pageHeight + paddingBetweenPages) * (pageNumber - 1) + y * scale
      _y -= 100
      $('#viewer').parent()[0].scrollTop = _y

      // highlight.
      annotation.highlight()
      setTimeout(() => annotation.dehighlight(), 1000)
    }
  }

  /**
   * Get the viewport of the viewer.
   */
  getViewerViewport() {
    return window.PDFView.pdfViewer.getPageView(0).viewport
  }

  /**
   * Load PDF data from url.
   * @memberof PDFAnnoPage
   */
  loadPdf(url: String): Promise<Uint8Array> {
    // add noise to the query parameters so caching is prevented
    var antiCacheUrl = url + "&time=" + new Date().getTime();
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

  /**
   * Load pdftxt data from url.
   * @memberof PDFAnnoPage
   */
  loadPdftxt(url: string): Promise<String> {
    // add noise to the query parameters so caching is prevented
    var antiCacheUrl = url + "&time=" + new Date().getTime();
    return fetch(antiCacheUrl, {
      method: 'GET',
      mode: 'cors'
    }).then(response => {
      if (response.ok) {
        return response.text()
      } else {
        throw new Error(`HTTP ${response.status} - pdftxt`)
      }
    })
  }

  /**
   * Load PDF and pdftxt from url.
   * @memberof PDFAnnoPage
   */
  loadPDFFromServer(pdfURL: string, pdftxtURL: string): Promise<Object> {
    return Promise.all([
      this.loadPdf(pdfURL),
      this.loadPdftxt(pdftxtURL)
    ]).then(results => {
      return {
        pdf: results[0],
        analyzeResult: results[1]
      }
    })
  }
}
