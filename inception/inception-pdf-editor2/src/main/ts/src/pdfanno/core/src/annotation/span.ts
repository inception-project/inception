import AbstractAnnotation from './abstract'
import { getGlyphsInRange } from '../../../page/textLayer'
import { Rectangle } from '../../../../vmodel/Rectangle'
import { mergeRects } from '../UI/span'

let clickCount = 0
let timer = null
const CLICK_DELAY = 300

/**
 * Span Annotation.
 */
export default class SpanAnnotation extends AbstractAnnotation {
  rectangles: Rectangle[] = []
  readOnly = false
  knob = true
  border = true
  text: string
  textRange: [number, number]
  page: number = -1
  zIndex = 10
  element: HTMLElement

  constructor () {
    super()

    this.type = 'span'
    this.vid = null
    this.color = null
    this.textRange = null
    this.page = null
    this.element = this.createDummyElement()

    // Need to bind these event handler methods
    this.handleClickEvent = this.handleClickEvent.bind(this)
    this.handleHoverInEvent = this.handleHoverInEvent.bind(this)
    this.handleHoverOutEvent = this.handleHoverOutEvent.bind(this)

    window.globalEvent.on('enableViewMode', this.enableViewMode)
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
  render (): boolean {
    if (!this.rectangles || this.rectangles.length === 0) {
      if (!this.page || !this.textRange) {
        console.error('ERROR: span missing page or textRange. span=', this)
        return false
      }
      this.rectangles = mergeRects(getGlyphsInRange(this.textRange))
    }

    return super.render()
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    this.element.querySelectorAll('.anno-knob').forEach(e => {
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

    window.globalEvent.removeListener('enableViewMode', this.enableViewMode)
    return promise
  }

  /**
   * Get the position for text.
   */
  getTextPosition () {
    let p = null

    if (this.rectangles.length > 0) {
      p = {
        x: this.rectangles[0].x + 7,
        y: this.rectangles[0].y - 20
      }
    }

    return p
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
    this.element.dispatchEvent(event)
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

    this.element.querySelectorAll('.anno-knob').forEach(e =>
      e.addEventListener('click', this.handleClickEvent))
  }

  handleClickEvent (ev: Event) {
    clickCount++
    if (clickCount === 1) {
      timer = setTimeout(() => {
        try {
          this.handleSingleClickEvent(ev) // perform single-click action
        } finally {
          clickCount = 0 // after action performed, reset counter
        }
      }, CLICK_DELAY)
    } else {
      clearTimeout(timer) // prevent single-click action
      try {
        this.handleDoubleClickEvent() // perform double-click action
      } finally {
        clickCount = 0 // after action performed, reset counter
      }
    }
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.element.querySelectorAll('.anno-knob').forEach(e =>
      e.removeEventListener('click', this.handleClickEvent))
  }
}
