import { EventEmitter } from 'events'
import { DEFAULT_RADIUS } from '../render/renderKnob'
import { dispatchWindowEvent } from '../../../shared/util'
import { type Annotation, AnnotationOutEvent, AnnotationOverEvent, type VID } from '@inception-project/inception-js-api'

/**
 * Abstract Annotation Class.
 */
export default abstract class AbstractAnnotation {
  private eventEmitter: EventEmitter // Use delegation instead of inheritance
  vid: VID | null = null
  source: Annotation | null = null
  color?: string
  deleted = false
  disabled = false
  readOnly = false
  hoverEventDisable = false
  selectedTime = null
  element: HTMLElement | null = null
  type: 'span' | 'relation'
  classList: string[] = []

  constructor() {
    this.eventEmitter = new EventEmitter() // Initialize the EventEmitter instance
    this.autoBind()
  }

  /**
   * Bind the `this` scope of instance methods to `this`.
   */
  autoBind() {
    Object.getOwnPropertyNames(this.constructor.prototype)
      .filter((prop) => typeof this[prop] === 'function')
      .forEach((method) => {
        this[method] = this[method].bind(this)
      })
  }

  /**
   * Delegate `on` to the internal EventEmitter.
   */
  on(event: string, listener: (...args: any[]) => void): void {
    this.eventEmitter.on(event, listener)
  }

  /**
   * Delegate `off` to the internal EventEmitter.
   */
  off(event: string, listener: (...args: any[]) => void): void {
    this.eventEmitter.off(event, listener)
  }

  /**
   * Delegate `emit` to the internal EventEmitter.
   */
  emit(event: string, ...args: any[]): void {
    this.eventEmitter.emit(event, ...args)
  }

  abstract setHoverEvent(): void

  /**
   * Render annotation(s).
   */
  render (): void {
    if (this.element) {
      this.element.remove()
      this.element = null
    }

    if (this.deleted) {
      return
    }

    const base = document.getElementById('annoLayer2')
    if (!base) {
      console.error('annoLayer2 could not be found')
      return
    }

    this.element = this.appendChild(base)
    if (!this.element) {
      return
    }

    if (!this.hoverEventDisable && this.setHoverEvent) {
      this.setHoverEvent()
    }

    if (!this.disabled) this.disable()
  }

  /**
   * Save the annotation data.
   */
  save () {
    globalThis.annotationContainer.add(this)
  }

  /**
   * Delete the annotation from rendering, a container in window, and a container in localStorage.
   */
  destroy () {
    this.deleted = true
    if (this.element) {
      this.element.remove()
      this.element = null
    }

    const promise = Promise.resolve()

    if (this.vid) {
      globalThis.annotationContainer.remove(this)
    }

    return promise
  }

  /**
   * Handle a left-click event.
   */
  handleSingleClickEvent (e: Event) {
    if (this.element) {
      const event = document.createEvent('CustomEvent')
      event.initCustomEvent('annotationSelected', true, true, this)
      this.element.dispatchEvent(event)
    }
  }

  /**
   * Handle a right-click event.
   */
  handleRightClickEvent (e: Event) {
    if (this.element && e instanceof MouseEvent) {
      // If the user shift-right-clicks, open the normal browser context menu. This is useful
      // e.g. during debugging / developing
      if (e.shiftKey) return

      e.preventDefault()

      const event = document.createEvent('CustomEvent')
      event.initCustomEvent('openContextMenu', true, true, { ann: this, originalEvent: e})
      this.element.dispatchEvent(event)
    }
  }
  
  /**
   * Handle a hoverIn event.
   */
  handleHoverInEvent (e: MouseEvent) {
    // console.log('handleHoverInEvent', this.element)

    this.highlight()
    this.emit('hoverin')
    dispatchWindowEvent('annotationHoverIn', this)

    if (this.element && this.source) {
      this.element.dispatchEvent(new AnnotationOverEvent(this.source, e))
    }
  }

  /**
   * Handle a hoverOut event.
   */
  handleHoverOutEvent (e: MouseEvent) {
    // console.log('handleHoverOutEvent')
    this.dehighlight()
    this.emit('hoverout')
    dispatchWindowEvent('annotationHoverOut', this)

    if (this.element && this.source) {
      this.element.dispatchEvent(new AnnotationOutEvent(this.source, e))
    }
  }

  /**
   * Highlight the annotation.
   */
  highlight () {
    this.element?.classList.add('--hover')
  }

  /**
   * Dehighlight the annotation.
   */
  dehighlight () {
    this.element?.classList.remove('--hover')
  }

  /**
   * Get the central position of the boundingCircle.
   */
  getBoundingCirclePosition () {
    const knob = this.element?.querySelector('.anno-knob') as HTMLElement | null
    if (!knob) {
      return null
    }

    return {
      x: parseFloat(knob.style.left) + DEFAULT_RADIUS / 2.0,
      y: parseFloat(knob.style.top) + DEFAULT_RADIUS / 2.0
    }
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
    if (this.element) {
      this.element.style.pointerEvents = 'auto'
    }
  }

  disable () {
    this.disabled = true
    if (this.element) {
      this.element.style.pointerEvents = 'none'
    }
  }

  /**
   * Check the another annotation is equal to `this`.
   */
  abstract equalTo(anotherAnnotation: any): boolean;

  abstract appendChild (base: HTMLElement): HTMLElement | null;
}
