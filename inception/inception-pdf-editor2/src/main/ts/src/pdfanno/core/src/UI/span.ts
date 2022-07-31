import { scaleDown } from './utils'
import SpanAnnotation from '../annotation/span'
import { findCharacterOffset, getGlyphsInRange, findGlyphAtPoint, Rect } from '../../../page/textLayer'
import { Rectangle } from '../../../../vmodel/Rectangle'
import { VGlyph } from '../../../../vmodel/VGlyph'

function scale () {
  return window.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
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

    if (tmp.p !== rect.p) {
      console.log('Page switch')
    }

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
  viewer.addEventListener('mousedown', handleMouseDown)
  viewer.addEventListener('mousemove', handleMouseMove)
  viewer.addEventListener('mouseup', handleMouseUp)
  viewer.addEventListener('click', handleClick)
  window.addEventListener('annotationHoverIn', () => otherAnnotationTreating = true)
  window.addEventListener('annotationHoverOut', () => otherAnnotationTreating = false)
  window.addEventListener('annotationDeleted', () => otherAnnotationTreating = false)
}

function setPositions (e: MouseEvent) {
  const canvasElement = (e.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasElement) {
    return
  }

  const pageElement = canvasElement.parentElement
  const page = parseInt(pageElement.getAttribute('data-page-number'))
  currentPage = page

  const { top, left } = canvasElement.getBoundingClientRect()
  const x = e.clientX - left
  const y = e.clientY - top

  const glyph = findGlyphAtPoint(page, scaleDown({ x, y }))
  if (glyph) {
    if (!selectionBegin || !selectionEnd) {
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

  currentSelectionHighlight = selectionHighlight([selectionBegin, selectionEnd], currentPage)
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
      document.getElementById('viewer').dispatchEvent(event)
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
    const page = parseInt(pageElement.getAttribute('data-page-number'))
    currentPage = page
    const { top, left } = canvasElement.getBoundingClientRect()
    const x = e.clientX - left
    const y = e.clientY - top

    const position = findCharacterOffset(page, scaleDown({ x, y }))

    if (position == null) {
      console.log('Position is null, cannot create zero-width span annotaton')
      return
    }

    const event = new CustomEvent('createSpanAnnotation', {
      bubbles: true,
      detail: { begin: position, end: position }
    })
    document.getElementById('viewer').dispatchEvent(event)

    // wait a second before destroying selection for better user experience
    setTimeout(function () {
      if (currentSelectionHighlight) {
        currentSelectionHighlight.destroy()
      }
    }, 1000)
    currentPage = null
  }
}

function selectionHighlight (textRange: [number, number], page: number): SpanAnnotation {
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
let initPosition = null
let selectionBegin: number = null
let selectionEnd: number = null
let currentPage: number = null
let currentSelectionHighlight: SpanAnnotation = null
let otherAnnotationTreating = false
