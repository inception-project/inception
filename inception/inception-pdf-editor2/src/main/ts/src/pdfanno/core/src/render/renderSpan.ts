import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import SpanAnnotation from '../annotation/span'
import { Rectangle } from '../../../../vmodel/Rectangle'

export function mapToDocumentCoordinates (aRectangles: Rectangle[]): Rectangle[] | undefined {
  if (!aRectangles) {
    return undefined
  }

  const firstPageElement = document.querySelector('.page')
  if (!firstPageElement) {
    console.warn('No page elements found')
    return undefined
  }

  const paddingTop = 9
  const pageView = window.PDFViewerApplication.pdfViewer.getPageView(0)
  const scale = pageView.viewport.scale
  const marginBetweenPages = 1 // TODO Where does this 1 come from?!

  const rectangles : Rectangle[] = []
  for (let r of aRectangles) {
    const pageContainer = document.querySelector(`.page[data-page-number="${r.p}"]`)
    if (!pageContainer) {
      console.warn(`No page element found for page ${r.p}`)
      return undefined
    }

    const pageTopY = pageContainer.offsetTop / scale + paddingTop + marginBetweenPages
    let leftOffset = 0

    if (firstPageElement.clientWidth > pageContainer.clientWidth) {
      const firstContainerStyle = getComputedStyle(firstPageElement)
      const firstPageLeft = parseInt(firstContainerStyle.marginLeft) + parseInt(firstContainerStyle.borderLeftWidth)
      const currentPageStyle = getComputedStyle(pageContainer)
      const currentPageLeft = parseInt(currentPageStyle.marginLeft) + parseInt(currentPageStyle.borderLeftWidth)
      leftOffset = (currentPageLeft - firstPageLeft) / scale
    }

    r = new Rectangle({ p: r.p, x: r.x + leftOffset, y: r.y + pageTopY, w: r.w, h: r.h })
    if (r.w > 0 && r.h > 0 /*&& r.x > -1 && r.y > -1*/) {
      rectangles.push(r)
    }
  }

  if (rectangles.length === 0) {
    console.warn(`${aRectangles.length} Input rectangles reduced to nothing`, aRectangles)
    return undefined
  }

  return rectangles
}

/**
 * Create a Span element.
 *
 * @param span - span annotation.
 * @return a html element describing a span annotation.
 */
export function renderSpan (span: SpanAnnotation): HTMLElement | undefined {
  const rectangles = mapToDocumentCoordinates(span.rectangles)

  if (!rectangles) {
    return undefined
  }

  const base = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = '10'

  rectangles.forEach(r => {
    base.appendChild(createRect(span, r, span.color, span.readOnly))
  })

  if (span.knob) {
    base.appendChild(renderKnob({
      a: span,
      x: rectangles[0].x,
      y: rectangles[0].y,
      readOnly: span.readOnly,
      text: span.text
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

  a?.classList.forEach(c => rect.classList.add(c))

  rect.style.top = r.y + 'px'
  rect.style.left = r.x + 'px'
  rect.style.width = r.w + 'px'
  rect.style.height = r.h + 'px'
  if (color) {
    rect.style.backgroundColor = hex2rgba(color, 0.4)
    rect.style.borderColor = color
  }
  rect.style.pointerEvents = 'none'
  return rect
}
