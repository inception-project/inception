import { scaleDown } from './utils'
import SpanAnnotation from '../annotation/span'
import { getGlyphsInRange, findGlyphAtPoint } from '../../../page/textLayer'
import { Rectangle } from '../../../../vmodel/Rectangle'
import { VGlyph } from '../../../../vmodel/VGlyph'

function scale () {
  return globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
}

/**
 * Merge user selections.
 */
export function mergeRects (glyphs: VGlyph[]) {
  if (glyphs.length === 0) {
    return []
  }

  // a virtical margin of error.
  const error = 5 * scale()

  let tmp = new Rectangle(glyphs[0].bbox)
  const newRects = [tmp]
  for (let i = 1; i < glyphs.length; i++) {
    const rect = glyphs[i].bbox

    // if (tmp.p !== rect.p) {
    //   console.log('Page switch')
    // }

    if (tmp.p === rect.p && withinMargin(rect.top, tmp.top, error)) {
      // Same line/same page -> Merge rects.
      tmp.setPosition({
        top: Math.min(tmp.top, rect.top),
        left: Math.min(tmp.left, rect.left),
        right: Math.max(tmp.right, rect.right),
        bottom: Math.max(tmp.bottom, rect.bottom)
      })
    } else {
      // New line/new page -> Create a new rect.
      tmp = new Rectangle(rect)
      newRects.push(tmp)
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

/**
 * Get the rect area of User selected.
 */
export function getRectangles () {
  if (!currentPage || !selectionBegin || !selectionEnd) {
    return null
  }

  return mergeRects(getGlyphsInRange([selectionBegin, selectionEnd]))
}

export function installSpanSelection () {
  const viewer = document.getElementById('viewer')
  if (viewer) {
    viewer.addEventListener('mousedown', handleMouseDown)
    viewer.addEventListener('mousemove', handleMouseMove)
    viewer.addEventListener('mouseup', handleMouseUp)
    viewer.addEventListener('click', handleClick)
  }
  window.addEventListener('annotationHoverIn', () => { otherAnnotationTreating = true })
  window.addEventListener('annotationHoverOut', () => { otherAnnotationTreating = false })
  window.addEventListener('annotationDeleted', () => { otherAnnotationTreating = false })
}

function setPositions (e: MouseEvent) {
  const canvasElement = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasElement) {
    console.error('No canvas element found.')
    return
  }

  const pageElement = canvasElement.parentElement
  if (!pageElement) {
    console.error('No page element found.')
    return
  }

  const pageNumberAttribute = pageElement.getAttribute('data-page-number')
  if (!pageNumberAttribute) {
    console.error('No page number attribute found.')
    return
  }

  const page = parseInt(pageNumberAttribute)
  currentPage = page

  const { top, left } = canvasElement.getBoundingClientRect()
  const x = e.clientX - left
  const y = e.clientY - top

  const glyph = findGlyphAtPoint(page, scaleDown({ x, y }))
  if (glyph) {
    if (selectionBegin === null || selectionEnd === null || initPosition === null) {
      initPosition = glyph.begin
      selectionBegin = glyph.begin
      selectionEnd = glyph.end
    } else {
      if (glyph.begin < initPosition) {
        selectionBegin = glyph.begin
        selectionEnd = initPosition
      } else {
        selectionBegin = initPosition
        selectionEnd = glyph.end
      }
    }
  }
}

function makeSelections (e: MouseEvent) {
  setPositions(e)

  if (currentSelectionHighlight) {
    currentSelectionHighlight.destroy()
    currentSelectionHighlight = null
  }

  if (selectionBegin !== null && selectionEnd !== null && currentPage !== null) {
    currentSelectionHighlight = selectionHighlight([selectionBegin, selectionEnd], currentPage)
  }
}

function handleMouseDown (e: MouseEvent) {
  const canvasWrapper = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasWrapper) {
    return
  }

  if (otherAnnotationTreating || e.shiftKey) {
    // Ignore, if other annotation is detected.
    return
  }
  mouseDown = true
  currentPage = null
  initPosition = null
  selectionBegin = null
  selectionEnd = null
  if (currentSelectionHighlight) {
    currentSelectionHighlight.destroy()
    currentSelectionHighlight = null
  }
  makeSelections(e)
}

function handleMouseMove (e: MouseEvent) {
  const canvasWrapper = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasWrapper) {
    return
  }

  if (mouseDown) {
    makeSelections(e)
  }
}

function handleMouseUp (e: MouseEvent) {
  const canvasWrapper = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasWrapper) {
    return
  }

  if (mouseDown) {
    makeSelections(e)

    if (selectionBegin !== null && selectionEnd !== null) {
      const event = new CustomEvent('createSpanAnnotation', {
        bubbles: true,
        detail: { begin: selectionBegin, end: selectionEnd }
      })

      document.getElementById('viewer')?.dispatchEvent(event)

      // wait a second before destroying selection for better user experience
      setTimeout(function () {
        if (currentSelectionHighlight) {
          currentSelectionHighlight.destroy()
        }
      }, 1000)
    }
  }

  mouseDown = false
}

function handleClick (e: MouseEvent) {
  const canvasElement = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasElement) {
    return
  }

  if (e.shiftKey) {
    const pageElement = canvasElement.parentElement
    if (!pageElement) {
      console.error('No page element found.')
      return
    }

    const pageNumberAttribute = pageElement.getAttribute('data-page-number')
    if (!pageNumberAttribute) {
      console.error('No page number attribute found.')
      return
    }

    const page = parseInt(pageNumberAttribute)
    currentPage = page
    const { top, left } = canvasElement.getBoundingClientRect()
    const x = e.clientX - left
    const y = e.clientY - top

    const glyph = findGlyphAtPoint(page, scaleDown({ x, y }))
    if (!glyph) {
      console.log(`No glyph at point ${[x, y]} - cannot create zero-width annotation`)
      return
    }

    const event = new CustomEvent('createSpanAnnotation', {
      bubbles: true,
      detail: { begin: glyph.begin, end: glyph.begin }
    })
    document.getElementById('viewer')?.dispatchEvent(event)

    // wait a second before destroying selection for better user experience
    setTimeout(function () {
      if (currentSelectionHighlight) {
        currentSelectionHighlight.destroy()
      }
    }, 1000)
    currentPage = null
  }
}

function selectionHighlight (textRange: [number, number], page: number): SpanAnnotation | null {
  const hl = new SpanAnnotation()
  hl.color = '#0f0'
  hl.textRange = textRange
  hl.page = page
  hl.knob = false
  hl.border = false
  hl.rectangles = mergeRects(getGlyphsInRange(textRange))

  if (hl.rectangles.length === 0) {
    return null
  }

  hl.render()
  hl.disable()
  return hl
}

let mouseDown = false
let initPosition: number | null = null
let selectionBegin: number | null = null
let selectionEnd: number | null = null
let currentPage: number | null = null
let currentSelectionHighlight: SpanAnnotation | null = null
let otherAnnotationTreating = false
