import AbstractAnnotation from './abstract'
import { anyOf } from '../../../shared/util'
import SpanAnnotation from './span'

/**
 * Relation Annotation (one-way / two-way / link)
 */
export default class RelationAnnotation extends AbstractAnnotation {
  text: string
  x1: number
  y1: number
  x2: number
  y2: number
  zIndex: number
  _rel1Annotation: SpanAnnotation
  _rel2Annotation: SpanAnnotation

  /**
   * Constructor.
   */
  constructor () {
    super()

    this.type = 'relation'
    this.rel1Annotation = null
    this.rel2Annotation = null
    this.text = null
    this.color = null
    this.readOnly = false
    this.element = this.createDummyElement()

    // for render.
    this.x1 = 0
    this.y1 = 0
    this.x2 = 0
    this.y2 = 0

    // Need to bind these event handler methods
    this.handleSingleClickEvent = this.handleSingleClickEvent.bind(this)
    this.handleHoverInEvent = this.handleHoverInEvent.bind(this)
    this.handleHoverOutEvent = this.handleHoverOutEvent.bind(this)

    window.globalEvent.on('enableViewMode', this.enableViewMode)
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    this.element.querySelectorAll('path').forEach(e => {
      e.addEventListener('mouseenter', this.handleHoverInEvent)
      e.addEventListener('mouseleave', this.handleHoverOutEvent)
    })
  }

  /**
   * Setter - rel1Annotation.
   */
  set rel1Annotation (a) {
    this._rel1Annotation = a
    if (this._rel1Annotation) {
      this._rel1Annotation.on('hoverin', this.handleRelHoverIn)
      this._rel1Annotation.on('hoverout', this.handleRelHoverOut)
      this._rel1Annotation.on('delete', this.handleRelDelete)
    }
  }

  /**
   * Getter - rel1Annotation.
   */
  get rel1Annotation () {
    return this._rel1Annotation
  }

  /**
   * Setter - rel2Annotation.
   */
  set rel2Annotation (a) {
    this._rel2Annotation = a
    if (this._rel2Annotation) {
      this._rel2Annotation.on('hoverin', this.handleRelHoverIn)
      this._rel2Annotation.on('hoverout', this.handleRelHoverOut)
      // this._rel2Annotation.on('rectmove', this.handleRelMove)
      this._rel2Annotation.on('delete', this.handleRelDelete)
    }
  }

  /**
   * Getter - rel2Annotation.
   */
  get rel2Annotation () {
    return this._rel2Annotation
  }

  /**
   * Render the annotation.
   */
  render (): boolean {
    this.setStartEndPosition()
    return super.render()
  }

  /**
   * Destroy the annotation.
   */
  destroy () {
    const promise = super.destroy()

    if (this._rel1Annotation) {
      this._rel1Annotation.removeListener('hoverin', this.handleRelHoverIn)
      this._rel1Annotation.removeListener('hoverout', this.handleRelHoverOut)
      this._rel1Annotation.removeListener('delete', this.handleRelDelete)
      delete this._rel1Annotation
    }

    if (this._rel2Annotation) {
      this._rel2Annotation.removeListener('hoverin', this.handleRelHoverIn)
      this._rel2Annotation.removeListener('hoverout', this.handleRelHoverOut)
      this._rel2Annotation.removeListener('delete', this.handleRelDelete)
      delete this._rel2Annotation
    }

    window.globalEvent.removeListener('enableViewMode', this.enableViewMode)

    return promise
  }

  /**
   * Highlight relations.
   */
  highlightRelAnnotations () {
    if (this._rel1Annotation) {
      this._rel1Annotation.highlight()
    }
    if (this._rel2Annotation) {
      this._rel2Annotation.highlight()
    }
  }

  /**
   * Dehighlight relations.
   */
  dehighlightRelAnnotations () {
    if (this._rel1Annotation) {
      this._rel1Annotation.dehighlight()
    }
    if (this.rel2Annotation) {
      this.rel2Annotation.dehighlight()
    }
  }

  /**
   * The callback for the relational text hoverred in.
   */
  handleTextHoverIn () {
    this.highlight()
    this.emit('hoverin')
    this.highlightRelAnnotations()
  }

  /**
   * The callback for the relational text hoverred out.
   */
  handleTextHoverOut () {
    this.dehighlight()
    this.emit('hoverout')
    this.dehighlightRelAnnotations()
  }

  /**
   * The callback for the relationals hoverred in.
   */
  handleRelHoverIn () {
    this.highlight()
    this.highlightRelAnnotations()
  }

  /**
   * The callback for the relationals hoverred out.
   */
  handleRelHoverOut () {
    this.dehighlight()
    this.dehighlightRelAnnotations()
  }

  /**
   * The callback that is called relations has benn deleted.
   */
  handleRelDelete () {
    this.destroy()
  }

  /**
   * The callback that is called relations has finished to be moved.
   */
  handleRelMoveEnd (rectAnnotation) {
    if (this._rel1Annotation === rectAnnotation || this._rel2Annotation === rectAnnotation) {
      this.enableViewMode()
    }
  }

  /**
   * The callback that is called at hoverred in.
   */
  handleHoverInEvent (e) {
    super.handleHoverInEvent(e)
    this.highlightRelAnnotations()
  }

  /**
   * The callback that is called at hoverred out.
   */
  handleHoverOutEvent (e) {
    super.handleHoverOutEvent(e)
    this.dehighlightRelAnnotations()
  }

  /**
   * Enable view mode.
   */
  enableViewMode () {
    this.disableViewMode()

    super.enableViewMode()

    if (!this.readOnly) {
      this.element.querySelectorAll('path').forEach(e =>
        e.addEventListener('click', this.handleSingleClickEvent))
    }
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.element.querySelectorAll('path').forEach(e =>
      e.removeEventListener('click', this.handleSingleClickEvent))
  }

  /**
   * Set the start / end points of the relation.
   */
  setStartEndPosition () {
    if (this._rel1Annotation) {
      const p = this._rel1Annotation.getBoundingCirclePosition()
      this.x1 = p.x
      this.y1 = p.y
    }
    if (this._rel2Annotation) {
      const p = this._rel2Annotation.getBoundingCirclePosition()
      this.x2 = p.x
      this.y2 = p.y
    }
  }

  equalTo (anno: RelationAnnotation) {
    if (!anno || this.type !== anno.type) {
      return false
    }

    const isSame = anyOf(this.rel1Annotation.vid, [anno.rel1Annotation.vid, anno.rel2Annotation.vid]) &&
      anyOf(this.rel2Annotation.vid, [anno.rel1Annotation.vid, anno.rel2Annotation.vid])

    return isSame
  }
}
