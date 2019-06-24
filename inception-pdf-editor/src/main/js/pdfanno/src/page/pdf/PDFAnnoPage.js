import * as annoUI from 'anno-ui'
import { getSearchHighlight } from '../search'
import { anyOf, dispatchWindowEvent } from '../../shared/util'
import { convertToExportY, paddingBetweenPages, nextZIndex } from '../../shared/coords'
import {
  unlistenWindowLeaveEvent,
  adjustViewerSize
} from '../util/window'
import * as constants from '../../shared/constants'

/**
 * PDFAnno's Annotation functions for Page produced by .
 */
export default class PDFAnnoPage {

  constructor () {
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

    // Alias for convenience.
    // TODO Remove this alias.
    window.iframeWindow = window

    // Adjust the height of viewer.
    adjustViewerSize()

    // Reset the confirm dialog at leaving page.
    unlistenWindowLeaveEvent()

    dispatchWindowEvent('iframeReady')
  }

  /**
   * Load files(contents and annoFiles).
   *
   * @param {Array<File>} files - files user selected in a file dialog.
   * @return {Promise}
   */
  loadFiles (files) {
    return loadFiles(files).then(result => {
      this.contentFiles = result.contents.map(c => {
        return Object.assign(c, {
          selected : false
        })
      })
      this.annoFiles = result.annos.map(a => {
        return Object.assign(a, {
          primary   : false,
          reference : false
        })
      })
    })
  }

  getContentFile (name) {
    const items = this.contentFiles.filter(c => c.name === name)
    if (items.length > 0) {
      return items[0]
    }
    return null
  }

  getAnnoFile (name) {
    const items = this.annoFiles.filter(c => c.name === name)
    if (items.length > 0) {
      return items[0]
    }
    return null
  }

  displayContent (contentName) {

    let contentFile = this.contentFiles.filter(c => c.name === contentName)
    if (contentFile.length === 0) {
      console.log('displayContent: NOT FOUND FILE. file=', contentName)
      return
    }

    this.displayViewer(contentFile[0])
  }

  displayViewer (contentFile) {

    // Reset settings.
    this.resetPDFViewerSettings()

    // Load PDF.
    const uint8Array = new Uint8Array(contentFile.content)
    window.iframeWindow.PDFViewerApplication.open(uint8Array)

    // Set the PDF file name.
    window.iframeWindow.PDFView.url = contentFile.name

    // Save the current.
    this.currentContentFile = contentFile
  }

  setCurrentContentFile (contentFile) {
    this.currentContentFile = contentFile
  }

  getCurrentContentFile () {
    return this.currentContentFile
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
    if (window.iframeWindow && window.iframeWindow.PDFViewerApplication) {
      window.iframeWindow.PDFViewerApplication.close()
      $('#numPages', window.iframeWindow.document).text('')
      this.currentContentFile = null
      dispatchWindowEvent('didCloseViewer')
    }
  }

  /**
   * Reset the setting of PDFViewer.
   */
  resetPDFViewerSettings () {
    localStorage.removeItem('database')
  }

  /**
   * Create a Span annotation.
   */
  createSpan ({ text = null, color = null } = {}) {
    // Get user selection.
    const rects = window.iframeWindow.PDFAnnoCore.default.UI.getRectangles()
    console.log('createSpan:rects:', rects)

    // Get a search result, if exists.
    const highlight = getSearchHighlight()

    // Get selected annotations.
    const selectedAnnotations = window.iframeWindow.annotationContainer.getSelectedAnnotations()

    // Check empty.
    if (!rects && !highlight && selectedAnnotations.length === 0) {
      console.log('check:', rects)
      return annoUI.ui.alertDialog.show({ message : 'Select text span or an annotation.' })
    }

    // Change color and label.
    if (selectedAnnotations.length > 0) {
      selectedAnnotations
        .filter(anno => anno.type === 'span')
        .forEach(anno => {
          anno.color = color
          anno.text = text
          anno.render()
          anno.enableViewMode()
        })
      dispatchWindowEvent('disappearTextInput')

      // Create a new rectAnnotation.
    } else if (rects) {
      window.iframeWindow.PDFAnnoCore.default.UI.createSpan({ text, zIndex : nextZIndex(), color })

    } else if (highlight) {

      const span = window.saveSpan({
        page         : highlight.page,
        rects        : highlight.rectangles,
        text,
        zIndex       : nextZIndex(),
        color,
        textRange    : highlight.textRange,
        selectedText : highlight.selectedText
      })
      this.addAnnotation(span)

      var event = document.createEvent('CustomEvent')
      event.initCustomEvent('enableTextInput', true, true, {
        uuid      : span.uuid,
        text      : text,
        autoFocus : true
      })
      window.dispatchEvent(event)
    }

    // Notify annotation added.
    dispatchWindowEvent('annotationrendered')
  }

  /**
   * Create a Relation annotation.
   */
  createRelation ({ type, text = null, color = null } = {}) {

    // for old style.
    if (arguments.length === 1 && typeof arguments[0] === 'string') {
      type = arguments[0]
    }

    // If a user select relation annotation(s), change the color and text only.
    const relAnnos = window.iframeWindow.annotationContainer.getSelectedAnnotations()
      .filter(anno => anno.type === 'relation')
    if (relAnnos.length > 0) {
      relAnnos
        .filter(anno => anno.direction === type)
        .forEach(anno => {
          anno.text = text
          anno.color = color
          anno.render()
          anno.enableViewMode()
        })
      return
    }

    let selectedAnnotations = window.iframeWindow.annotationContainer.getSelectedAnnotations()
    selectedAnnotations = selectedAnnotations.filter(a => {
      return a.type === 'span'
    }).sort((a1, a2) => {
      return (a1.selectedTime - a2.selectedTime) // asc
    })

    if (selectedAnnotations.length < 2) {
      return annoUI.ui.alertDialog.show({ message : 'Two annotated text spans are not selected.\nTo select multiple annotated spans, click the first annotated span, then Ctrl+Click (Windows) or Cmd+Click (OSX) the second span.' })
    }

    const first  = selectedAnnotations[selectedAnnotations.length - 2]
    const second = selectedAnnotations[selectedAnnotations.length - 1]
    console.log('first:second,', first, second)

    // Check duplicated.
    const arrows = window.iframeWindow.annotationContainer
      .getAllAnnotations()
      .filter(a => a.type === 'relation')
      .filter(a => {
        return anyOf(a.rel1Annotation.uuid, [first.uuid, second.uuid])
          && anyOf(a.rel2Annotation.uuid, [first.uuid, second.uuid])
      })

    if (arrows.length > 0) {
      console.log('same found!!!')
      // Update!!
      arrows[0].direction = type
      arrows[0].rel1Annotation = first
      arrows[0].rel2Annotation = second
      arrows[0].text = text
      arrows[0].color = color || arrows[0].color
      arrows[0].save()
      arrows[0].render()
      arrows[0].enableViewMode()
      // Show label input.
      var event = document.createEvent('CustomEvent')
      event.initCustomEvent('enableTextInput', true, true, {
        uuid : arrows[0].uuid,
        text : arrows[0].text
      })
      window.dispatchEvent(event)
      return
    }

    window.iframeWindow.PDFAnnoCore.default.UI.createRelation({
      type,
      anno1 : first,
      anno2 : second,
      text,
      color
    })

    // Notify annotation added.
    dispatchWindowEvent('annotationrendered')
  }

  /**
   * Display annotations an user selected.
   */
  displayAnnotation (isPrimary) {

    // Check the viewer not clised.
    if ($('#numPages', window.iframeWindow.document).text() === '') {
      return
    }

    const colorMap = annoUI.labelInput.getColorMap()

    let annotations = []
    let primaryIndex = -1

    // Primary annotation.
    if (isPrimary) {
      $('#dropdownAnnoPrimary a').each((index, element) => {
        let $elm = $(element)
        if ($elm.find('.fa-check').hasClass('no-visible') === false) {
          let annoPath = $elm.find('.js-annoname').text()

          const annoFile = window.annoPage.getAnnoFile(annoPath)
          if (!annoFile) {
            console.log('ERROR')
            return
          }
          primaryIndex = 0
          annotations.push(annoFile.content)

          let filename = annoFile.name
          localStorage.setItem('_pdfanno_primary_annoname', filename)
          console.log('filename:', filename)
        }
      })
    }

    // Reference annotations.
    if (!isPrimary) {
      $('#dropdownAnnoReference a').each((index, element) => {
        let $elm = $(element)
        if ($elm.find('.fa-check').hasClass('no-visible') === false) {
          let annoPath = $elm.find('.js-annoname').text()

          const annoFile = window.annoPage.getAnnoFile(annoPath)

          if (!annoFile) {
            console.log('ERROR')
            return
          }
          annotations.push(annoFile.content)
        }
      })
    }

    // Create import data.
    let paperData = {
      primary : primaryIndex,
      annotations,
      colorMap
    }

    // Import annotations to Viewer.
    window.annoPage.importAnnotation(paperData, isPrimary)
  }

  /**
   * Get all annotations.
   */
  getAllAnnotations () {
    if (!window.iframeWindow || !window.iframeWindow.annotationContainer) {
      return []
    }
    return window.iframeWindow.annotationContainer.getAllAnnotations()
  }

  /**
   * Get selected annotations.
   */
  getSelectedAnnotations () {
    return window.iframeWindow.annotationContainer.getSelectedAnnotations()
  }

  /**
   * Find an annotation by id.
   */
  findAnnotationById (id) {
    return window.iframeWindow.annotationContainer.findById(id)
  }

  /**
   * Clear the all annotations from the view and storage.
   */
  clearAllAnnotations () {
    if (window.iframeWindow) {
      window.iframeWindow.annotationContainer.getAllAnnotations().forEach(a => a.destroy())
    }
  }

  /**
   * Add an annotation to the container.
   */
  addAnnotation  (annotation) {
    window.iframeWindow.annotationContainer.add(annotation)
  }

  /**
   * Create a new span annotation.
   */
  createSpanAnnotation (options) {
    console.log('createSpanAnnotation:', options)
    return window.iframeWindow.PDFAnnoCore.default.SpanAnnotation.newInstance(options)
  }

  /**
   * Create a new relation annotation.
   */
  createRelationAnnotation (options) {
    return window.iframeWindow.PDFAnnoCore.default.RelationAnnotation.newInstance(options)
  }

  validateSchemaErrors (errors) {
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
  importAnnotation (paperData, isPrimary) {
    window.iframeWindow.annotationContainer.importAnnotations(paperData, isPrimary).then(() => {
      // Notify annotations added.
      dispatchWindowEvent('annotationrendered')
    }).catch(errors => {
      let message = errors
      if (Array.isArray(errors)) {
        message = this.validateSchemaErrors(errors)
      }
      annoUI.ui.alertDialog.show({ message })
    })
  }

  /**
   * Scroll window to the annotation.
   */
  scrollToAnnotation (id) {

    let annotation = window.annoPage.findAnnotationById(id)

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
      let pageHeight = window.annoPage.getViewerViewport().height
      let scale = window.annoPage.getViewerViewport().scale
      let _y = (pageHeight + paddingBetweenPages) * (pageNumber - 1) + y * scale
      _y -= 100
      $('#viewer').parent()[0].scrollTop = _y

      // highlight.
      annotation.highlight()
      setTimeout(() => {
        annotation.dehighlight()
      }, 1000)
    }
  }

  /**
   * Get the export data of annotations.
   *
   * @return {Promise}
   */
  exportData ({exportType = 'toml'} = {}) {
    return window.iframeWindow.annotationContainer.exportData(...arguments)
  }

  /**
   * Get the viewport of the viewer.
   */
  getViewerViewport () {
    return window.iframeWindow.PDFView.pdfViewer.getPageView(0).viewport
  }

  /**
   * Load PDF data from url.
   * @param {String} url
   * @returns Promise<Uint8Array>
   * @memberof PDFAnnoPage
   */
  loadPdf (url) {
    // add noise to the query parameters so caching is prevented
    var antiCacheUrl= url + "&time=" + new Date().getTime();
    return fetch(antiCacheUrl, {
      method : 'GET',
      mode   : 'cors'
    }).then(response => {
      if (response.ok) {
        return response.arrayBuffer()
      } else {
        // throw new Error(`HTTP ${response.status} - ${response.statusText}`)
        throw new Error(`HTTP ${response.status} - PDFファイルのロードに失敗しました。`)
      }
    }).then(buffer => {
      return new Uint8Array(buffer)
    })
  }

  /**
   * Load pdftxt data from url.
   * @param {String} url
   * @returns Promise<String>
   * @memberof PDFAnnoPage
   */
  loadPdftxt (url) {
    // add noise to the query parameters so caching is prevented
    var antiCacheUrl= url + "&time=" + new Date().getTime();
    return fetch(antiCacheUrl, {
      method : 'GET',
      mode   : 'cors'
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
   * @param {String} pdfURL
   * @param {String} pdftxtURL
   * @returns Promise<Object>
   * @memberof PDFAnnoPage
   */
  loadPDFFromServer (pdfURL, pdftxtURL) {
    return Promise.all([
      this.loadPdf(pdfURL),
      this.loadPdftxt(pdftxtURL)
    ]).then(results => {
      return {
        pdf           : results[0],
        analyzeResult : results[1]
      }
    })
  }

  set pdftxt (text) {
    this._pdftxt = text
  }

  get pdftxt () {
    return this._pdftxt
  }

}
