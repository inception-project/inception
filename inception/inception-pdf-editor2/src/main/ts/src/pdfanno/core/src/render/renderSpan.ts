import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'
import SpanAnnotation from '../model/SpanAnnotation'
import { Rectangle } from '../../../../vmodel/Rectangle'
import { scale } from '../../../page/textLayer'
import { VGlyph } from '../../../../vmodel/VGlyph'

export function mapToDocumentCoordinates (aRectangles: Rectangle[]): Rectangle[] | undefined {
  if (!aRectangles || aRectangles.length === 0) {
    return undefined
  }

  const firstPageElement = document.querySelector('.page')
  if (!firstPageElement) {
    console.warn('No page elements found')
    return undefined
  }

  const firstPageLeft = firstPageElement.getBoundingClientRect().left

  const paddingTop = 9
  const pageView = globalThis.PDFViewerApplication.pdfViewer.getPageView(0)
  const scale = pageView.viewport.scale
  const marginBetweenPages = 1 // TODO Where does this 1 come from?!

  // Build a cache of all the pages we need and their positions
  const pageMap = new Map();
  aRectangles.forEach((r) => {
    if (pageMap.has(r.p)) return
    const pageContainer = document.querySelector(`.page[data-page-number="${r.p}"]`) as HTMLElement
    const rect = pageContainer.getBoundingClientRect()
    pageMap.set(r.p, { container: pageContainer, left: rect.left })
  })

  const rectangles : Rectangle[] = []
  for (let r of aRectangles) {
    const pageInfo = pageMap.get(r.p);
    if (!pageInfo) {
      console.warn(`No page element found for page ${r.p}`)
      return undefined
    }

    const pageContainer = pageInfo.container
    const pageTopY = pageContainer.offsetTop / scale + paddingTop + marginBetweenPages
    let leftOffset = 0

    // If the pages are not rendered left-aligned because one is wider than the other, we need
    // to adjust the position of the rectangle on the X-axis
    leftOffset = (pageInfo.left - firstPageLeft) / scale

    // console.log(leftOffset, r.x + leftOffset, r.y + pageTopY, r.w, r.h)
    r = new Rectangle({ p: r.p, x: r.x + leftOffset, y: r.y + pageTopY, w: r.w, h: r.h })
    if (r.w > 0 && r.h > 0 /* && r.x > -1 && r.y > -1 */) {
      rectangles.push(r)
    }
  }

  if (rectangles.length === 0) {
    console.warn(`${aRectangles.length} input rectangles reduced to nothing`, aRectangles)
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
export function renderSpan (span: SpanAnnotation): HTMLElement | null {
  const rectangles = mapToDocumentCoordinates(span.rectangles)

  if (!rectangles || rectangles.length === 0) {
    console.warn('No rectangles found for span annotation', span)
    return null
  }

  const base = document.createElement('div')
  base.classList.add('anno-span')
  base.style.zIndex = '10'

  rectangles.forEach(r => {
    base.appendChild(createRect(r, span, span.color, span.readOnly))
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

export function createRect (r: Rectangle, a?: SpanAnnotation, color?: string, readOnly?: boolean): HTMLElement {
  const rect = document.createElement('div')
  rect.classList.add(readOnly ? 'anno-span__border' : 'anno-span__area')

  if (!(a?.border)) {
    rect.classList.add('no-border')
  }

  a?.classList.forEach(c => rect.classList.add(c))

  const defaultBorderWidth = 2 // px
  const clippedScale = Math.max(scale(), 0.25)
  const scaledBorderWidth = defaultBorderWidth / Math.max(0.5 * clippedScale, 1.0)
  const borderHeightAdjustment = defaultBorderWidth - scaledBorderWidth;

  // As the scaling factor increases, we can gradually reduce the height of the highlight in order
  // for it to overlap less with the next line. Empirically, log10 seems to work better than log
  // for this.
  const scaleHeightAdjustment = Math.log10(Math.max(1.0, scale()))

  rect.style.top = `${r.y}px`
  rect.style.left = `${r.x}px`
  rect.style.width = `${r.w}px`
  rect.style.height = `${r.h - borderHeightAdjustment - scaleHeightAdjustment}px`
  rect.style.setProperty('--marker-focus-width', `${scaledBorderWidth}px`);

  if (color) {
    rect.style.backgroundColor = hex2rgba(color, 0.4)
    rect.style.borderColor = hex2rgba(color, 0.8)
    rect.style.borderWidth = `${scaledBorderWidth}px`
  }

  rect.style.pointerEvents = 'none'
  return rect
}

/**
 * Merge the rectangles of the glyphs into fewer rectangles.
 */
export function mergeRects (glyphs: VGlyph[]): Rectangle[] {
  if (glyphs.length === 0) {
    return []
  }

  // a vertical margin of error.
  const baseError = 2.5 // px

  let aggregateRect = new Rectangle(glyphs[0].bbox)

  const newRects = [aggregateRect]
  for (let i = 1; i < glyphs.length; i++) {
    const glyphBox = glyphs[i].bbox
    const error = Math.min(baseError, glyphBox.h)

    if (aggregateRect.p === glyphBox.p && withinMargin(glyphBox.top, aggregateRect.top, error)) {
      // Same line/same page -> Merge rects.
      aggregateRect.setPosition({
        top: Math.min(aggregateRect.top, glyphBox.top),
        left: Math.min(aggregateRect.left, glyphBox.left),
        right: Math.max(aggregateRect.right, glyphBox.right),
        bottom: Math.max(aggregateRect.bottom, glyphBox.bottom)
      })
    } else {
      // New line/new page -> Create a new rect.
      aggregateRect = new Rectangle(glyphBox)
      newRects.push(aggregateRect)
    }
  }

  return newRects
}

/**
 * Check the value(x) within the range.
 */
function withinMargin (x: number, base: number, margin: number) {
  return (base - margin) <= x && x <= (base + margin)
}
