import AbstractAnnotation from './AbstractAnnotation'
import { getGlyphsInRange } from '../../../page/textLayer'
import { Rectangle } from '../../../../vmodel/Rectangle'
import { mergeRects, renderSpan } from '../render/renderSpan'
import { renderRelation } from '../render/renderRelation'
import { transform } from '../UI/utils'

let clickCount = 0
let timer : number | null = null

/**
 * Number of milliseconds in which a click event is detected as a double-click event
 */
const CLICK_DELAY = 300

/**
 * Span Annotation.
 */
export default class SpanAnnotation extends AbstractAnnotation {
  rectangles: Rectangle[] = []
  knob = true
  border = true
  text: string
  textRange: [number, number]
  page: number
  zIndex = 10

  constructor () {
    super()

    this.type = 'span'

    // Need to bind these event handler methods
    this.handleClickEvent = this.handleClickEvent.bind(this)
    this.handleRightClickEvent = this.handleRightClickEvent.bind(this)
    this.handleHoverInEvent = this.handleHoverInEvent.bind(this)
    this.handleHoverOutEvent = this.handleHoverOutEvent.bind(this)

    globalThis.globalEvent.on('enableViewMode', this.enableViewMode)
  }

  equalTo (anno: SpanAnnotation) {
    if (!anno || this.type !== anno.type) {
      return false
    }

    return this.vid === anno.vid
  }

  /**
   * Render annotation(s).
   */
  render (): void {
    if (!this.rectangles || this.rectangles.length === 0) {
      if (!this.page || !this.textRange) {
        console.error('ERROR: span missing page or textRange. span=', this)
        return
      }
      this.rectangles = mergeRects(getGlyphsInRange(this.textRange))
    }

    super.render()
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    this.element?.querySelectorAll('.anno-knob').forEach(e => {
      e.addEventListener('mouseenter', this.handleHoverInEvent)
      e.addEventListener('mouseleave', this.handleHoverOutEvent)
    })
  }

  /**
   * Delete the annotation from rendering, a container in window, and a container in localStorage.
   */
  destroy () {
    const promise = super.destroy()
    this.emit('delete')

    globalThis.globalEvent.removeListener('enableViewMode', this.enableViewMode)
    return promise
  }

  /**
   * Handle a hovein event on a text.
   */
  handleTextHoverIn () {
    this.highlight()
    this.emit('hoverin')
  }

  /**
   * Handle a hoveout event on a text.
   */
  handleTextHoverOut () {
    this.dehighlight()
    this.emit('hoverout')
  }

  handleDoubleClickEvent () {
    const event = new CustomEvent('doubleClickAnnotation', {
      bubbles: true,
      detail: this
    })
    this.element?.dispatchEvent(event)
  }

  /**
   * Enable view mode.
   */
  enableViewMode () {
    this.disableViewMode()
    super.enableViewMode()
    if (this.readOnly) {
      return
    }

    this.element?.querySelectorAll('.anno-knob').forEach(e => {
      e.addEventListener('mousedown', this.preventFocusChange)
      e.addEventListener('click', this.handleClickEvent)
      e.addEventListener('contextmenu', this.handleRightClickEvent)
    })
  }

  /**
   * We do not want the focus to leave the feature editors when clicking on a knob, so we need
   * to prevent the default action for 'mousedown' events.
   */
  private preventFocusChange (ev: Event) {
    ev.preventDefault()
  }

  /**
   * Detect if a click is a single-click or a double-click and trigger the corresponding action.
   */
  handleClickEvent (ev: Event) {
    ev.stopPropagation()
    ev.preventDefault()
    clickCount++
    if (clickCount === 1) {
      timer = window.setTimeout(() => {
        try {
          this.handleSingleClickEvent(ev) // perform single-click action
        } finally {
          clickCount = 0 // after action performed, reset counter
        }
      }, CLICK_DELAY)
    } else {
      if (timer !== null) {
        clearTimeout(timer) // prevent single-click action
        try {
          this.handleDoubleClickEvent() // perform double-click action
        } finally {
          clickCount = 0 // after action performed, reset counter
        }
      }
    }
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.element?.querySelectorAll('.anno-knob').forEach(e => {
      e.removeEventListener('click', this.handleClickEvent)
      e.removeEventListener('contextmenu', this.handleRightClickEvent)
      e.removeEventListener('mousedown', this.preventFocusChange)
    })
  }

  appendChild (base: HTMLElement): HTMLElement | null {
    let child: HTMLElement | null
    child = renderSpan(this)

    // If no type was provided for an annotation it will result in node being null.
    // Skip appending/transforming if node doesn't exist.
    if (child) {
      const viewport = globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport
      const elm = transform(base, child, viewport)
      base.append(elm)
    }

    return child
  }
}
