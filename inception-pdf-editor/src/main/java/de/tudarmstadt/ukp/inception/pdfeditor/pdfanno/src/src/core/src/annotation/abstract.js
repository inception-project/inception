import EventEmitter from 'events'
import appendChild from '../render/appendChild'
import { DEFAULT_RADIUS } from '../render/renderKnob'
import { dispatchWindowEvent } from '../utils/event'

/**
 * Abstract Annotation Class.
 */
export default class AbstractAnnotation extends EventEmitter {

  /**
   * Check the argument is an annotation.
   */
  static isAnnotation (obj) {
    return obj && obj.uuid && obj.type
  }

  /**
   * Constructor.
   */
  constructor () {
    super()
    this.autoBind()
    this.deleted = false
    this.selected = false
    this.selectedTime = null
    this.createdAt = new Date().getTime()
  }

  /**
   * Bind the `this` scope of instance methods to `this`.
   */
  autoBind () {
    Object.getOwnPropertyNames(this.constructor.prototype)
      .filter(prop => typeof this[prop] === 'function')
      .forEach(method => {
        this[method] = this[method].bind(this)
      })
  }

  /**
   * Render annotation(s).
   */
  render () {

    this.$element.remove()

    if (this.deleted) {
      return false
    }

    const base = $('#annoLayer2')[0]
    this.$element = $(appendChild(base, this))

    if (!this.hoverEventDisable && this.setHoverEvent) {
      this.setHoverEvent()
    }

    this.selected && this.$element.addClass('--selected')

    this.disabled && this.disable()

    return true
  }

  /**
   * Save the annotation data.
   */
  save () {
    window.annotationContainer.add(this)
  }

  /**
   * Delete the annotation from rendering, a container in window, and a container in localStorage.
   */
  destroy () {
    this.deleted = true
    this.$element.remove()

    let promise = Promise.resolve()

    if (this.uuid) {
      window.annotationContainer.remove(this)
    }

    return promise
  }

  /**
   * Judge the point within the element.
   */
  isHit (x, y) {
    return false
  }

  /**
   * Handle a click event.
   */
  handleClickEvent (e) {
    this.toggleSelect()

    if (this.type !== 'textbox') {

      if (this.selected) {

        // TODO Use common function.
        let event = document.createEvent('CustomEvent')
        event.initCustomEvent('annotationSelected', true, true, this)
        window.dispatchEvent(event)

      } else {

        // TODO Use common function.
        let event = document.createEvent('CustomEvent')
        event.initCustomEvent('annotationDeselected', true, true, this)
        window.dispatchEvent(event)

      }
    }
  }

  /**
   * Handle a hoverIn event.
   */
  handleHoverInEvent (e) {
    console.log('handleHoverInEvent')
    this.highlight()
    this.emit('hoverin')
    dispatchWindowEvent('annotationHoverIn', this)
  }

  /**
   * Handle a hoverOut event.
   */
  handleHoverOutEvent (e) {
    console.log('handleHoverOutEvent')
    this.dehighlight()
    this.emit('hoverout')
    dispatchWindowEvent('annotationHoverOut', this)
  }

  /**
   * Highlight the annotation.
   */
  highlight () {
    this.$element.addClass('--hover')
  }

  /**
   * Dehighlight the annotation.
   */
  dehighlight () {
    this.$element.removeClass('--hover')
  }

  /**
   * Select the annotation.
   */
  select () {
    this.selected = true
    this.selectedTime = Date.now()
    this.$element.addClass('--selected')
  }

  /**
   * Deselect the annotation.
   */
  deselect () {
    console.log('deselect')
    this.selected = false
    this.selectedTime = null
    this.$element.removeClass('--selected')
  }

  /**
   * Toggle the selected state.
   */
  toggleSelect () {

    if (this.selected) {
      this.deselect()
    } else {
      this.select()
    }

  }

  /**
   * Delete the annotation if selected.
   */
  deleteSelectedAnnotation () {

    if (this.isSelected()) {
      this.destroy().then(() => {
        dispatchWindowEvent('annotationDeleted', { uuid : this.uuid })
      })
      return true
    }
    return false
  }

  /**
   * Check whether the annotation is selected.
   */
  isSelected () {
    return this.$element.hasClass('--selected')
  }

  /**
   * Create a dummy DOM element for the timing that a annotation hasn't be specified yet.
   */
  createDummyElement () {
    return $('<div class="dummy"/>')
  }

  /**
   * Get the central position of the boundingCircle.
   */
  getBoundingCirclePosition () {
    const $circle = this.$element.find('.anno-knob')
    if ($circle.length > 0) {
      return {
        // x : parseFloat($circle.css('left')) + parseFloat($circle.css('width')) / 2,
        // y : parseFloat($circle.css('top')) + parseFloat($circle.css('height')) / 2
        x : parseFloat($circle.css('left')) + DEFAULT_RADIUS / 2.0,
        y : parseFloat($circle.css('top')) + DEFAULT_RADIUS / 2.0
      }
    }
    return null
  }

  /**
   * Enable a view mode.
   */
  enableViewMode () {
    this.render()
  }

  /**
   * Disable a view mode.
   */
  disableViewMode () {
    this.render()
  }

  setDisableHoverEvent () {
    this.hoverEventDisable = true
  }

  setEnableHoverEvent () {
    this.hoverEventDisable = false
  }

  enable () {
    this.disabled = false
    this.$element.css('pointer-events', 'auto')
  }

  disable () {
    this.disabled = true
    this.$element.css('pointer-events', 'none')
  }

  /**
   * Check the another annotation is equal to `this`.
   */
  equalTo (anotherAnnotation) {
    // Implement Here.
    return false
  }
}
