import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import SpanAnnotation from '../annotation/span'

/**
 * Create a Span element.
 *
 * @param a - span annotation.
 * @return a html element describing a span annotation.
 */
export function renderSpan (a: SpanAnnotation, svg): HTMLElement {
  const readOnly = a.readOnly

  const color = a.color || '#FF0'

  const base = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = `${a.zIndex || 10}`

  if (!a.page) {
    if (a.rectangles.length > 0) {
      a.page = a.rectangles[0].page
    }
  }

  const paddingTop = 9
  const pageView = window.PDFViewerApplication.pdfViewer.getPageView(0)
  const viewport = pageView.viewport
  const scale = viewport.scale
  const marginBetweenPages = 1
  const pageContainer = document.querySelector(`.page[data-page-number="${a.page}"]`)
  const pageTopY = pageContainer.offsetTop / scale + paddingTop + marginBetweenPages

  const rectangles = a.rectangles.map(r => {
    return {
      x: r.x || r.left,
      y: (r.y || r.top) + pageTopY,
      width: r.width || r.right - r.left,
      height: r.height || r.bottom - r.top
    }
  }).filter(r => r.width > 0 && r.height > 0 && r.x > -1 && r.y > -1)

  rectangles.forEach(r => {
    base.appendChild(createRect(a, r, color, readOnly))
  })

  if (a.knob) {
    base.appendChild(renderKnob({
      x: rectangles[0].x,
      y: rectangles[0].y,
      readOnly,
      text: a.text,
      color: a.color
    }))
  }

  return base
}

function createRect (a, r, color, readOnly): HTMLElement {
  const rect = document.createElement('div')
  rect.classList.add(readOnly ? 'anno-span__border' : 'anno-span__area')
  if (a.border === false) {
    rect.classList.add('no-border')
  }

  rect.style.top = r.y + 'px'
  rect.style.left = r.x + 'px'
  rect.style.width = r.width + 'px'
  rect.style.height = r.height + 'px'
  rect.style.backgroundColor = hex2rgba(color, 0.4)
  rect.style.borderColor = color
  rect.style.pointerEvents = 'none'
  return rect
}
