import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import { ANNO_VERSION } from '../version'

console.log('ANNO_VERSION:', ANNO_VERSION)

/**
 * Create a Span element.
 * @param {SpanAnnotation} a - span annotation.
 * @return {HTMLElement} a html element describing a span annotation.
 */
export function renderSpan (a) {

  const readOnly = a.readOnly

  const color = a.color || '#FF0'

  const $base = $('<div class="anno-span"/>')
    .css('zIndex', a.zIndex || 10)

  if (!a.page) {
    if (a.rectangles.length > 0) {
      a.page = a.rectangles[0].page
    }
  }

  let paddingTop = 9
  const pageView = window.PDFView.pdfViewer.getPageView(0)
  const viewport = pageView.viewport
  const scale = viewport.scale
  let merginBetweenPages =  1
  let pageTopY = $('#pageContainer' + a.page).position().top / scale + paddingTop + merginBetweenPages

  const rectangles = a.rectangles.map(r => {
    return {
      x      : r.x || r.left,
      y      : (r.y || r.top) + pageTopY,
      width  : r.width || r.right - r.left,
      height : r.height || r.bottom - r.top
    }
  }).filter(r => r.width > 0 && r.height > 0 && r.x > -1 && r.y > -1)

  rectangles.forEach(r => {
    $base.append(createRect(a, r, color, readOnly))
  })

  if (a.knob) {
    $base.append(renderKnob({
      x : rectangles[0].x,
      y : rectangles[0].y,
      readOnly,
      text : a.text,
      color : a.color
    }))
  }

  return $base[0]
}

function createRect (a, r, color, readOnly) {
  let className = readOnly ? 'anno-span__border' : 'anno-span__area'
  if (a.border === false) {
    className += ' no-border'
  }

  return $(`<div class="${className}"/>`).css({
    top             : r.y + 'px',
    left            : r.x + 'px',
    width           : r.width + 'px',
    height          : r.height + 'px',
    backgroundColor : hex2rgba(color, 0.4),
    borderColor     : color
  })
}
