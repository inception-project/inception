import AbstractAnnotation from './AbstractAnnotation'
import { anyOf } from '../../../shared/util'
import SpanAnnotation from './SpanAnnotation'
import { renderRelation } from '../render/renderRelation'
import { transform } from '../UI/utils'

/**
 * Relation Annotation (one-way / two-way / link)
 */
export default class RelationAnnotation extends AbstractAnnotation {
  text: string | null = null
  x1: number
  y1: number
  x2: number
  y2: number
  zIndex: number
  _rel1Annotation: SpanAnnotation | null = null
  _rel2Annotation: SpanAnnotation | null = null

  /**
   * Constructor.
   */
  constructor () {
    super()

    this.type = 'relation'

    // for render.
    this.x1 = 0
    this.y1 = 0
    this.x2 = 0
    this.y2 = 0

    // Need to bind these event handler methods
    this.handleSingleClickEvent = this.handleSingleClickEvent.bind(this)
    this.handleRightClickEvent = this.handleRightClickEvent.bind(this)
    this.handleHoverInEvent = this.handleHoverInEvent.bind(this)
    this.handleHoverOutEvent = this.handleHoverOutEvent.bind(this)

    globalThis.globalEvent.on('enableViewMode', this.enableViewMode)
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    if (this.element) {
      this.element.querySelectorAll('path').forEach(e => {
        e.addEventListener('mouseenter', this.handleHoverInEvent)
        e.addEventListener('mouseleave', this.handleHoverOutEvent)
      })
    }
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
  render (): void {
    this.setStartEndPosition()
    super.render()
  }

  /**
   * Destroy the annotation.
   */
  destroy () {
    const promise = super.destroy()

    if (this._rel1Annotation) {
      this._rel1Annotation.off('hoverin', this.handleRelHoverIn)
      this._rel1Annotation.off('hoverout', this.handleRelHoverOut)
      this._rel1Annotation.off('delete', this.handleRelDelete)
      this._rel1Annotation = null
    }

    if (this._rel2Annotation) {
      this._rel2Annotation.off('hoverin', this.handleRelHoverIn)
      this._rel2Annotation.off('hoverout', this.handleRelHoverOut)
      this._rel2Annotation.off('delete', this.handleRelDelete)
      this._rel2Annotation = null
    }

    globalThis.globalEvent.removeListener('enableViewMode', this.enableViewMode)

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
  handleHoverInEvent (e: MouseEvent) {
    super.handleHoverInEvent(e)
    this.highlightRelAnnotations()
  }

  /**
   * The callback that is called at hoverred out.
   */
  handleHoverOutEvent (e: MouseEvent) {
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
      this.element?.querySelectorAll('path').forEach(e => {
        e.addEventListener('click', this.handleSingleClickEvent)
        e.addEventListener('contextmenu', this.handleRightClickEvent)
    })}
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.element?.querySelectorAll('path').forEach(e => {
      e.removeEventListener('click', this.handleSingleClickEvent)
      e.removeEventListener('contextmenu', this.handleRightClickEvent)
    })
  }

  /**
   * Set the start / end points of the relation.
   */
  setStartEndPosition () {
    if (this._rel1Annotation) {
      const p = this._rel1Annotation.getBoundingCirclePosition()
      if (p !== null) {
        this.x1 = p.x
        this.y1 = p.y
      }
    }
    if (this._rel2Annotation) {
      const p = this._rel2Annotation.getBoundingCirclePosition()
      if (p !== null) {
        this.x2 = p.x
        this.y2 = p.y
      }
    }
  }

  equalTo (anno: RelationAnnotation) {
    if (!anno || this.type !== anno.type) {
      return false
    }

    const isSame =
      anyOf(this.rel1Annotation?.vid, [anno.rel1Annotation?.vid, anno.rel2Annotation?.vid]) &&
      anyOf(this.rel2Annotation?.vid, [anno.rel1Annotation?.vid, anno.rel2Annotation?.vid])

    return isSame
  }

  appendChild (base: HTMLElement): HTMLElement | null {
    let child: HTMLElement | null
        child = renderRelation(this)
  
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
