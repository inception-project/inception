import { scaleDown } from './utils'
import SpanAnnotation from '../model/SpanAnnotation'
import { getGlyphsInRange, findGlyphAtPointWithinPage } from '../../../page/textLayer'
import { mergeRects } from '../render/renderSpan'

let mouseDown = false
let initPosition: number | null = null
let selectionBegin: number | null = null
let selectionEnd: number | null = null
let currentPage: number | null = null
let currentSelectionHighlight: SpanAnnotation | null = null
let otherAnnotationTreating = false

export function installSpanSelection () {
  const viewer = document.getElementById('viewer')
  if (viewer) {
    viewer.addEventListener('mousedown', ev => handleMouseDown(ev))
    viewer.addEventListener('mousemove', ev => handleMouseMove(ev))
    viewer.addEventListener('mouseup', ev => handleMouseUp(ev))
    viewer.addEventListener('click', ev => handleClick(ev))
  }

  window.addEventListener('annotationHoverIn', () => { otherAnnotationTreating = true })
  window.addEventListener('annotationHoverOut', () => { otherAnnotationTreating = false })
  // See https://github.com/inception-project/inception/issues/4008
  window.addEventListener('doubleClickAnnotation', () => { otherAnnotationTreating = false })
}

function startSelection () {
  mouseDown = true
  currentPage = null
  initPosition = null
  selectionBegin = null
  selectionEnd = null
}

function clearSelection () {
  mouseDown = false
  currentPage = null
  initPosition = null
  selectionBegin = null
  selectionEnd = null

  // wait a moment before destroying selection for better user experience
  setTimeout(() => removeSelectionHighlight(), 500)
}

function updateSelection (ev: MouseEvent) {
  const canvasElement = (ev.target as HTMLElement).closest('.canvasWrapper')
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
  const x = ev.clientX - left
  const y = ev.clientY - top

  const glyph = findGlyphAtPointWithinPage(page, scaleDown({ x, y }))
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

  removeSelectionHighlight()

  if (selectionBegin !== null && selectionEnd !== null && currentPage !== null) {
    currentSelectionHighlight = renderSelectionHighlight([selectionBegin, selectionEnd], currentPage)
  }
}

/**
 * Check if the event happend within a PDF page's canvasWrapper element.
 * @param ev the event
 * @returns if the event is in scope
 */
function isWithinScope (ev: MouseEvent): boolean {
  const canvasWrapper = (ev.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasWrapper) {
    return false
  }

  return true
}

function handleMouseDown (ev: MouseEvent) {
  if (!isWithinScope(ev) || otherAnnotationTreating || ev.shiftKey || ev.detail > 1) {
    // Ignore, if other annotation is detected.
    return
  }

  startSelection()
  updateSelection(ev)
}

function handleMouseMove (ev: MouseEvent) {
  if (!isWithinScope(ev) || !mouseDown) {
    return
  }

  updateSelection(ev)
}

function handleMouseUp (ev: MouseEvent) {
  if (!isWithinScope(ev) || !mouseDown || ev.detail > 1) {
    return
  }

  updateSelection(ev)

  if (selectionBegin !== null && selectionEnd !== null) {
    const event = new CustomEvent('createSpanAnnotation', {
      bubbles: true,
      detail: { begin: selectionBegin, end: selectionEnd }
    })

    document.getElementById('viewer')?.dispatchEvent(event)

    clearSelection()
  }

  mouseDown = false
}

function handleClick (ev: MouseEvent) {
  const canvasElement = (ev.target as HTMLElement).closest('.canvasWrapper')
  if (!canvasElement || !ev.shiftKey || ev.detail > 1) {
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
  const x = ev.clientX - left
  const y = ev.clientY - top

  const glyph = findGlyphAtPointWithinPage(page, scaleDown({ x, y }))
  if (!glyph) {
    console.warn(`No glyph at point ${[x, y]} - cannot create zero-width annotation`)
    return
  }

  const event = new CustomEvent('createSpanAnnotation', {
    bubbles: true,
    detail: { begin: glyph.begin, end: glyph.begin }
  })
  document.getElementById('viewer')?.dispatchEvent(event)

  clearSelection()
}

function renderSelectionHighlight (textRange: [number, number], page: number): SpanAnnotation | null {
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

function removeSelectionHighlight () {
  if (currentSelectionHighlight) {
    currentSelectionHighlight.destroy()
    currentSelectionHighlight = null
  }
}
