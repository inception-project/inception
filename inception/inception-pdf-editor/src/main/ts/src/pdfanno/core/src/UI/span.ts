import { scaleDown } from './utils'
import SpanAnnotation from '../annotation/span'
import { findIndex, getGlyphsInRange, findGlyphAtPoint } from '../../../page/textLayer'

function scale () {
  return window.PDFViewerApplication.pdfViewer.getPageView(0).viewport.scale
}

/**
 * Merge user selections.
 */
function mergeRects (rects) {
  // Remove null.
  rects = rects.filter(rect => rect)

  // Normalize.
  rects = rects.map(rect => {
    rect.top = rect.top || rect.y
    rect.left = rect.left || rect.x
    rect.right = rect.right || (rect.x + rect.w)
    rect.bottom = rect.bottom || (rect.y + rect.h)
    return rect
  })

  // a vertical margin of error.
  const error = 5 * scale()

  let tmp = convertToObject(rects[0])
  const newRects = [tmp]
  for (let i = 1; i < rects.length; i++) {
    // Same line -> Merge rects.
    if (withinMargin(rects[i].top, tmp.top, error)) {
      tmp.top = Math.min(tmp.top, rects[i].top)
      tmp.left = Math.min(tmp.left, rects[i].left)
      tmp.right = Math.max(tmp.right, rects[i].right)
      tmp.bottom = Math.max(tmp.bottom, rects[i].bottom)
      tmp.x = tmp.left
      tmp.y = tmp.top
      tmp.width = tmp.right - tmp.left
      tmp.height = tmp.bottom - tmp.top

      // New line -> Create a new rect.
    } else {
      tmp = convertToObject(rects[i])
      newRects.push(tmp)
    }
  }

  return newRects
}

/**
 * Convert a DOMList to a javascript plan object.
 */
function convertToObject (rect) {
  return {
    top: rect.top,
    left: rect.left,
    right: rect.right,
    bottom: rect.bottom,
    x: rect.x,
    y: rect.y,
    width: rect.width,
    height: rect.height
  }
}

/**
 * Check the value(x) within the range.
 */
function withinMargin (x, base, margin) {
  return (base - margin) <= x && x <= (base + margin)
}

/**
 * Save a rect annotation.
 */
export function saveSpan ({
  text = '',
  rects = [],
  textRange = [],
  selectedText = '',
  zIndex = 10,
  color = '#ffff00',
  page = 1,
  save = true,
  focusToLabel = true,
  knob = true,
  border = true
}) {
  if (!rects) {
    return
  }

  const annotation = {
    rectangles: rects,
    selectedText,
    text,
    textRange,
    zIndex,
    color,
    page,
    knob,
    border
  }

  // Save.
  const spanAnnotation = SpanAnnotation.newInstance(annotation)
  if (save) {
    spanAnnotation.save()
  }

  // Render.
  spanAnnotation.render()

  // Select.
  spanAnnotation.select()

  return spanAnnotation
}
window.saveSpan = saveSpan

/**
 * Get the rect area of User selected.
 */
export function getRectangles () {
  if (!currentPage || !startPosition || !endPosition) {
    return null
  } else {
    const targets = getGlyphsInRange(currentPage, startPosition, endPosition)
    return mergeRects(targets)
  }
}

/**
 * Create a span by current texts selection.
 */
export function createSpan ({ text = null, zIndex = 10, color = null }) {
  if (!currentPage || !startPosition || !endPosition) {
    return null
  } else {
    const targets = getGlyphsInRange(currentPage, startPosition, endPosition)
    if (targets.length === 0) {
      return null
    }

    const selectedText = targets.map(t => {
      return t ? t.char : ' '
    }).join('')

    const mergedRect = mergeRects(targets)
    const annotation = saveSpan({
      rects: mergedRect,
      page: currentPage,
      text,
      zIndex,
      color,
      textRange: [startPosition, endPosition],
      selectedText
    })

    // Remove user selection.
    if (spanAnnotation) {
      spanAnnotation.destroy()
    }
    startPosition = null
    endPosition = null
    currentPage = null
    spanAnnotation = null

    return annotation
  }
}

export function installSpanSelection () {
  function setPositions (e) {
    const canvasElement = (e.target as HTMLElement).closest('.canvasWrapper')
    if (!canvasElement) {
      return
    }

    const pageElement = canvasElement.parentNode
    const page = parseInt(pageElement.getAttribute('data-page-number'))
    currentPage = page

    const { top, left } = canvasElement.getBoundingClientRect()
    const x = e.clientX - left
    const y = e.clientY - top

    // Find the data in pdftxt.
    const glyph = findGlyphAtPoint(page, scaleDown({ x, y }))
    if (glyph) {
      if (!startPosition || !endPosition) {
        initPosition = glyph.position
        startPosition = glyph.position
        endPosition = glyph.position
      } else {
        if (glyph.position < initPosition) {
          startPosition = glyph.position
          endPosition = initPosition
        } else {
          startPosition = initPosition
          endPosition = glyph.position
        }
      }
    }
  }

  function makeSelections (e) {
    setPositions(e)

    if (spanAnnotation) {
      spanAnnotation.destroy()
      spanAnnotation = null
    }

    const targets = getGlyphsInRange(currentPage, startPosition, endPosition)
    let selectedText = ''
    targets.forEach(m => selectedText += m.glyph)
    console.log(selectedText)
    if (targets.length > 0) {
      const mergedRect = mergeRects(targets)
      spanAnnotation = saveSpan({
        rects: mergedRect,
        page: currentPage,
        save: false,
        focusToLabel: false,
        color: '#0f0',
        knob: false,
        border: false,
        textRange: [startPosition, endPosition]
      })
      spanAnnotation.disable()
    }
  }

  const viewer = document.getElementById('viewer')
  const canvasWrappers = viewer.querySelectorAll('.canvasWrapper')

  viewer.addEventListener('mousedown', e => {
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
    startPosition = null
    endPosition = null
    if (spanAnnotation) {
      spanAnnotation.destroy()
      spanAnnotation = null
    }
    makeSelections(e)
  })

  viewer.addEventListener('mousemove', e => {
    const canvasWrapper = (e.target as HTMLElement).closest('.canvasWrapper')
    if (!canvasWrapper) {
      return
    }

    if (mouseDown) {
      makeSelections(e)
    }
  })

  viewer.addEventListener('mouseup', e => {
    const canvasWrapper = (e.target as HTMLElement).closest('.canvasWrapper')
    if (!canvasWrapper) {
      return
    }

    if (mouseDown) {
      makeSelections(e)
      if (spanAnnotation) {
        spanAnnotation.deselect()
      }
      if (startPosition !== null && endPosition !== null) {
        const event = new CustomEvent('createSpanAnnotation', {
          bubbles: true,
          detail: { begin: startPosition, end: endPosition + 1 }
        })
        viewer.dispatchEvent(event)
        // wait a second before destroying selection for better user experience
        setTimeout(function () {
          if (spanAnnotation) {
            spanAnnotation.destroy()
          }
        }, 1000)
      }
    }
    mouseDown = false
  })

  viewer.addEventListener('click', e => {
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

      const position = findIndex(page, scaleDown({ x, y }))

      if (position == null) {
        console.log('Position is null, cannot create zero-width span annotaton')
        return
      }

      const event = new CustomEvent('createSpanAnnotation', {
        bubbles: true,
        detail: { begin: position, end: position }
      })
      viewer.dispatchEvent(event)
      // wait a second before destroying selection for better user experience
      setTimeout(function () {
        if (spanAnnotation) {
          spanAnnotation.destroy()
        }
      }, 1000)
      currentPage = null
    }
  })

  let otherAnnotationTreating = false
  window.addEventListener('annotationHoverIn', () => {
    otherAnnotationTreating = true
  })
  window.addEventListener('annotationHoverOut', () => {
    otherAnnotationTreating = false
  })
  window.addEventListener('annotationDeleted', () => {
    otherAnnotationTreating = false
  })
}

let mouseDown = false
let initPosition = null
let startPosition: number = null
let endPosition: number = null
let currentPage: number = null
let spanAnnotation: SpanAnnotation = null
