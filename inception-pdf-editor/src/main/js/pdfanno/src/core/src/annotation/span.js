import { uuid } from 'anno-ui/src/utils'
import AbstractAnnotation from './abstract'
// import { convertFromExportY } from '../../../shared/coords'
// import appendChild from '../render/appendChild'

/**
 * Span Annotation.
 */
export default class SpanAnnotation extends AbstractAnnotation {

  /**
   * Constructor.
   */
  constructor () {
    super()

    this.uuid         = null
    this.type         = 'span'
    this.rectangles   = []
    this.text         = null
    this.color        = null
    this.readOnly     = false
    this.selectedText = null
    this.textRange    = null
    this.page         = null
    this.knob         = true
    this.border       = true
    this.$element     = this.createDummyElement()

    window.globalEvent.on('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
    window.globalEvent.on('enableViewMode', this.enableViewMode)
  }

  /**
   * Create an instance from an annotation data.
   */
  static newInstance (annotation) {
    let a          = new SpanAnnotation()
    a.uuid         = annotation.uuid || uuid()
    a.text         = annotation.text
    a.color        = annotation.color
    a.readOnly     = annotation.readOnly || false
    a.selectedText = annotation.selectedText
    a.textRange    = annotation.textRange
    a.page         = annotation.page
    a.zIndex       = annotation.zIndex || 10
    a.knob         = (typeof annotation.knob === 'boolean' ? annotation.knob : true)
    a.border       = annotation.border !== false

    // Calc the position.
    let rects = window.findTexts(a.page, a.textRange[0], a.textRange[1])
    rects = window.mergeRects(rects)
    a.rectangles = rects

    return a
  }

  /**
   * Create an instance from a TOML object.
   */
  static newInstanceFromTomlObject (tomlObject) {
    let d = tomlObject
    d.selectedText = d.text
    d.text = d.label
    d.textRange = d.textrange
    let span = SpanAnnotation.newInstance(d)
    return span
  }

  /**
   * Render annotation(s).
   */
  render () {

    if (!this.rectangles || this.rectangles.length === 0) {
      if (!this.page || !this.textRange) {
        return console.log('ERROR: span missing page or textRange. span=', this)
      }
      let rects = window.findTexts(this.page, this.textRange[0], this.textRange[1])
      rects = window.mergeRects(rects)
      this.rectangles = rects
    }

    return super.render()
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    this.$element.find('.anno-knob').hover(
      this.handleHoverInEvent,
      this.handleHoverOutEvent
    )
  }

  /**
   * Delete the annotation from rendering, a container in window, and a container in localStorage.
   */
  destroy () {
    let promise = super.destroy()
    this.emit('delete')

    // TODO オブジェクトベースで削除できるようにしたい.
    window.globalEvent.removeListener('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
    window.globalEvent.removeListener('enableViewMode', this.enableViewMode)
    return promise
  }

  /**
   * Create an annotation data for save.
   */
  createAnnotation () {
    return {
      uuid         : this.uuid,
      type         : this.type,
      rectangles   : this.rectangles,
      text         : this.text,
      color        : this.color,
      readyOnly    : this.readOnly,
      selectedText : this.selectedText
    }
  }

  /**
   * Get the position for text.
   */
  getTextPosition () {

    let p = null

    if (this.rectangles.length > 0) {
      p = {
        x : this.rectangles[0].x + 7,
        y : this.rectangles[0].y - 20
      }
    }

    return p
  }

  /**
   * Delete the annotation if selected.
   */
  deleteSelectedAnnotation () {
    super.deleteSelectedAnnotation()
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

  /**
   * Save a new text.
   */
  handleTextChanged (newText) {
    this.text = newText
    this.save()
  }

  /**
   * Handle a hoverin event.
   */
  handleHoverInEvent (e) {
    super.handleHoverInEvent(e)
    this.emit('circlehoverin', this)
  }

  /**
   * Handle a hoverout event.
   */
  handleHoverOutEvent (e) {
    super.handleHoverOutEvent(e)
    this.emit('circlehoverout', this)
  }

  /**
   * Handle a click event.
   */
  handleClickEvent (e) {
    super.handleClickEvent(e)
  }

  export (id) {

    let text = (this.selectedText || '')
      .replace(/\r\n/g, ' ')
      .replace(/\r/g, ' ')
      .replace(/\n/g, ' ')
      .replace(/"/g, '')
      .replace(/\\/g, '')

    return {
      id        : id + '',
      page      : this.page,
      label     : this.text || '',
      text,
      textrange : this.textRange
    }
  }

  export040 () {

    let text = (this.selectedText || '')
      .replace(/\r\n/g, ' ')
      .replace(/\r/g, ' ')
      .replace(/\n/g, ' ')
      .replace(/"/g, '')
      .replace(/\\/g, '')

    return {
      type      : this.type,
      page      : this.page,
      label     : this.text || '',
      text,
      textrange : this.textRange
    }
  }

  /**
   * Enable view mode.
   */
  enableViewMode () {
    this.disableViewMode()
    super.enableViewMode()
    if (!this.readOnly) {
      this.$element.find('.anno-knob').on('click', this.handleClickEvent)
    }
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.$element.find('.anno-knob').off('click')
  }
}
