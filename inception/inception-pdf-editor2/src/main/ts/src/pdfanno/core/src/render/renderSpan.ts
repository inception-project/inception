import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import SpanAnnotation from '../annotation/span'
import { Rectangle } from '../../../../vmodel/Rectangle'

export function mapToDocumentCoordinates (aRectangles: Rectangle[], page: number): Rectangle[] | undefined {
  if (!aRectangles) {
    return undefined
  }

  const firstPageElement = document.querySelector('.page')
  const pageContainer = document.querySelector(`.page[data-page-number="${page}"]`)

  if (!firstPageElement || !pageContainer) {
    return undefined
  }

  const paddingTop = 9
  const pageView = window.PDFViewerApplication.pdfViewer.getPageView(0)
  const scale = pageView.viewport.scale
  const marginBetweenPages = 1 // TODO Where does this 1 come from?!
  const pageTopY = pageContainer.offsetTop / scale + paddingTop + marginBetweenPages
  let leftOffset = 0

  if (firstPageElement.clientWidth > pageContainer.clientWidth) {
    const firstContainerStyle = getComputedStyle(firstPageElement)
    const firstPageLeft = parseInt(firstContainerStyle.marginLeft) + parseInt(firstContainerStyle.borderLeftWidth)
    const currentPageStyle = getComputedStyle(pageContainer)
    const currentPageLeft = parseInt(currentPageStyle.marginLeft) + parseInt(currentPageStyle.borderLeftWidth)
    leftOffset = (currentPageLeft - firstPageLeft) / scale
  }

  const rectangles = aRectangles
    .map(r => new Rectangle({ x: r.x + leftOffset, y: r.y + pageTopY, w: r.w, h: r.h }))
    .filter(r => r.w > 0 && r.h > 0 && r.x > -1 && r.y > -1)

  return rectangles
}

/**
 * Create a Span element.
 *
 * @param span - span annotation.
 * @return a html element describing a span annotation.
 */
export function renderSpan (span: SpanAnnotation): HTMLElement | undefined {
  // REC: This should not be needed
  // if (!span.page) {
  //   if (span.rectangles.length > 0) {
  //     span.page = span.rectangles[0].page
  //   }
  // }

  const rectangles = mapToDocumentCoordinates(span.rectangles, span.page)

  if (!rectangles) {
    return undefined
  }

  const color = span.color || '#FF0'

  const base = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = '10'

  rectangles.forEach(r => {
    base.appendChild(createRect(span, r, color, span.readOnly))
  })

  if (span.knob) {
    base.appendChild(renderKnob({
      x: rectangles[0].x,
      y: rectangles[0].y,
      readOnly: span.readOnly,
      text: span.text,
      color
    }))
  }

  return base
}

export function createRect (a: SpanAnnotation | undefined, r: Rectangle, color?: string, readOnly?: boolean): HTMLElement {
  const rect = document.createElement('div')
  rect.classList.add(readOnly ? 'anno-span__border' : 'anno-span__area')

  if (!a?.border) {
    rect.classList.add('no-border')
  }

  const actualColor = color || '#FF0'

  rect.style.top = r.y + 'px'
  rect.style.left = r.x + 'px'
  rect.style.width = r.w + 'px'
  rect.style.height = r.h + 'px'
  rect.style.backgroundColor = hex2rgba(actualColor, 0.4)
  rect.style.borderColor = actualColor
  rect.style.pointerEvents = 'none'
  return rect
}
