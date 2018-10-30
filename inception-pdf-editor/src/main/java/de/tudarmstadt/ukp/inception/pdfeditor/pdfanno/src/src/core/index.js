/**
 Functions for annotations rendered over a PDF file.
 */
require('!style-loader!css-loader!./index.css')
import { dispatchWindowEvent } from '../shared/util'

import EventEmitter from 'events'

window.globalEvent = new EventEmitter()
window.globalEvent.setMaxListeners(0)

// This is the entry point of window.xxx.
// (setting from webpack.config.js)
import PDFAnnoCore from './src/PDFAnnoCore'
export default PDFAnnoCore

// Create an annocation container.
import AnnotationContainer from './src/annotation/container'
window.annotationContainer = new AnnotationContainer()

// Enable a view mode.
PDFAnnoCore.UI.enableViewMode()

// The event called at page rendered by pdfjs.
window.addEventListener('pagerendered', function (ev) {
  console.log('pagerendered:', ev.detail.pageNumber)

  // No action, if the viewer is closed.
  if (!window.PDFView.pdfViewer.getPageView(0)) {
    return
  }

  adjustPageGaps()
  renderAnno()
})

// Adapt to scale change.
window.addEventListener('scalechange', () => {
  console.log('scalechange')
  adjustPageGaps()
  removeAnnoLayer()
  renderAnno()
})

function adjustPageGaps () {
  // Issue Fix.
  // Correctly rendering when changing scaling.
  // The margin between pages is fixed(9px), and never be scaled in default,
  // then manually have to change the margin.
  let scale = window.PDFView.pdfViewer.getPageView(0).viewport.scale
  let borderWidth = `${9 * scale}px`
  let marginBottom = `${-9 * scale}px`
  let marginTop = `${1 * scale}px`
  $('.page').css({
    'border-top-width'    : borderWidth,
    'border-bottom-width' : borderWidth,
    marginBottom,
    marginTop
  })
}

/*
 * Remove the annotation layer and the temporary rendering layer.
 */
function removeAnnoLayer () {
  // TODO Remove #annoLayer.
  $('#annoLayer, #annoLayer2').remove()
}

/*
 * Render annotations saved in the storage.
 */
function renderAnno () {

  // No action, if the viewer is closed.
  if (!window.PDFView.pdfViewer.getPageView(0)) {
    return
  }

  // TODO make it a global const.
  const svgLayerId = 'annoLayer'
  const annoLayer2Id = 'annoLayer2'

  // Check already exists.
  if ($('#' + svgLayerId).length > 0) {
    return
  }
  if ($('#' + annoLayer2Id).length > 0) {
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
    position   : 'absolute',
    top        : '0px',
    left       : `${leftMargin}px`,
    width      : `${width}px`,
    height     : `${height}px`,
    visibility : 'hidden',
    'z-index'  : 2
  })
  // Add an annotation layer.
  let $annoLayer2 = $(`<div id="${annoLayer2Id}"/>`).addClass('annoLayer').css({   // TODO CSSClass.
    position   : 'absolute',
    top        : '0px',
    left       : `${leftMargin}px`,
    width      : `${width}px`,
    height     : `${height}px`,
    visibility : 'hidden',
    'z-index'  : 2
  })

  $('#viewer').css({
    position : 'relative'  // TODO css.
  }).append($annoLayer).append($annoLayer2)

  dispatchWindowEvent('annotationlayercreated')

  renderAnnotations()
}

/**
 * Render all annotations.
 */
function renderAnnotations () {
  const annotations = window.annotationContainer.getAllAnnotations()
  if (annotations.length === 0) {
    return
  }
  annotations.forEach(a => {
    a.render()
    a.enableViewMode()
  })
  dispatchWindowEvent('annotationrendered')
}
