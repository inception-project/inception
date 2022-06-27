import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import SpanAnnotation from '../annotation/span'
import { Rectangle } from '../../../../vmodel/Rectangle'

/**
 * Create a Span element.
 *
 * @param span - span annotation.
 * @return a html element describing a span annotation.
 */
export function renderSpan (span: SpanAnnotation): HTMLElement | undefined {
  const readOnly = span.readOnly
  const color = span.color || '#FF0'
  const base = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = '10'

  if (!span.page) {
    if (span.rectangles.length > 0) {
      span.page = span.rectangles[0].page
    }
  }

  const paddingTop = 9
  const pageView = window.PDFViewerApplication.pdfViewer.getPageView(0)
  const viewport = pageView.viewport
  const scale = viewport.scale
  const marginBetweenPages = 1 // TODO Where does this 1 come from?!
  const firstPageElement = document.querySelector('.page')
  const pageContainer = document.querySelector(`.page[data-page-number="${span.page}"]`)

  if (firstPageElement === null || pageContainer === null) {
    return undefined
  }

  const pageTopY = pageContainer.offsetTop / scale + paddingTop + marginBetweenPages
  let leftOffset = 0

  if (firstPageElement.clientWidth > pageContainer.clientWidth) {
    const firstContainerStyle = getComputedStyle(firstPageElement)
    const currentPageStyle = getComputedStyle(pageContainer)
    const firstPageLeft = parseInt(firstContainerStyle.marginLeft) + parseInt(firstContainerStyle.borderLeftWidth)
    const currentPageLeft = parseInt(currentPageStyle.marginLeft) + parseInt(currentPageStyle.borderLeftWidth)
    leftOffset = (currentPageLeft - firstPageLeft) / scale
  }

  const rectangles = span.rectangles
    .map(r => new Rectangle({ x: r.x + leftOffset, y: r.y + pageTopY, w: r.w, h: r.h }))
    .filter(r => r.w > 0 && r.h > 0 && r.x > -1 && r.y > -1)

  rectangles.forEach(r => {
    base.appendChild(createRect(span, r, color, readOnly))
  })

  if (span.knob) {
    base.appendChild(renderKnob({
      x: rectangles[0].x,
      y: rectangles[0].y,
      readOnly,
      text: span.text,
      color: span.color
    }))
  }

  return base
}

function createRect (a: SpanAnnotation, r: Rectangle, color: string, readOnly: boolean): HTMLElement {
  const rect = document.createElement('div')
  rect.classList.add(readOnly ? 'anno-span__border' : 'anno-span__area')
  if (a.border === false) {
    rect.classList.add('no-border')
  }

  rect.style.top = r.y + 'px'
  rect.style.left = r.x + 'px'
  rect.style.width = r.w + 'px'
  rect.style.height = r.h + 'px'
  rect.style.backgroundColor = hex2rgba(color, 0.4)
  rect.style.borderColor = color
  rect.style.pointerEvents = 'none'
  return rect
}
