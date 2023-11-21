import AbstractAnnotation from './abstract'
import { mergeRects, getGlyphsInRange, Rect } from '../../../page/textLayer'

/**
 * Span Annotation.
 */
export default class SpanAnnotation extends AbstractAnnotation {
  rectangles: Rect[] = []
  readOnly = false
  knob = true
  border = true
  text = null
  selectedText = null
  textRange: [number, number] = null
  page: number = null
  zIndex: number
  element: HTMLElement = null

  /**
   * Constructor.
   */
  constructor () {
    super()

    this.type = 'span'
    this.uuid = null
    this.text = null
    this.color = null
    this.selectedText = null
    this.textRange = null
    this.page = null
    this.element = this.createDummyElement()

    // Need to bind these event handler methods
    this.handleClickEvent = this.handleClickEvent.bind(this)
    this.handleHoverInEvent = this.handleHoverInEvent.bind(this)
    this.handleHoverOutEvent = this.handleHoverOutEvent.bind(this)

    window.globalEvent.on('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
    window.globalEvent.on('enableViewMode', this.enableViewMode)
  }

  /**
   * Create an instance from an annotation data.
   */
  public static newInstance (annotation: SpanAnnotation, allowZeroWidth?: boolean) {
    const a = new SpanAnnotation()
    a.uuid = annotation.uuid
    a.text = annotation.text
    a.color = annotation.color
    a.readOnly = annotation.readOnly || false
    a.selectedText = annotation.selectedText
    a.textRange = annotation.textRange
    a.page = annotation.page
    a.zIndex = annotation.zIndex || 10
    a.knob = (typeof annotation.knob === 'boolean' ? annotation.knob : true)
    a.border = annotation.border !== false

    // Calc the position.
    a.rectangles = mergeRects(getGlyphsInRange(a.page, a.textRange[0], a.textRange[1], allowZeroWidth))

    return a
  }

  /**
   * Create an instance from a TOML object.
   */
  public static newInstanceFromTomlObject (tomlObject) {
    const d = tomlObject
    d.selectedText = d.text
    d.text = d.label
    d.textRange = d.textrange
    const span = SpanAnnotation.newInstance(d, true)
    return span
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
      this.rectangles = mergeRects(getGlyphsInRange(this.page, this.textRange[0], this.textRange[1]))
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

    window.globalEvent.removeListener('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
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
   * Delete the annotation if selected.
   */
  deleteSelectedAnnotation (): boolean {
    return super.deleteSelectedAnnotation()
  }

  /**
   * Handle a selected event on a text.
   */
  handleTextSelected () {
    this.select()
  }

  /**
   * Handle a deselected event on a text.
   */
  handleTextDeselected () {
    this.deselect()
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

var clickCount = 0
var timer = null
var CLICK_DELAY = 300
