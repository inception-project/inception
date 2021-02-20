import { uuid } from 'anno-ui/src/utils'
import AbstractAnnotation from './abstract'
import { getRelationTextPosition } from '../utils/relation.js'
import { anyOf } from '../../../shared/util'

let globalEvent

/**
 * Relation Annotation (one-way / two-way / link)
 */
export default class RelationAnnotation extends AbstractAnnotation {

  /**
   * Constructor.
   */
  constructor () {
    super()

    globalEvent = window.globalEvent

    this.uuid = uuid()
    this.type = 'relation'
    this.direction = null
    this.rel1Annotation = null
    this.rel2Annotation = null
    this.text = null
    this.color = null
    this.readOnly = false
    this.$element = this.createDummyElement()

    // for render.
    this.x1 = 0
    this.y1 = 0
    this.x2 = 0
    this.y2 = 0

    globalEvent.on('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
    globalEvent.on('enableViewMode', this.enableViewMode)
    globalEvent.on('rectmoveend', this.handleRelMoveEnd)
  }

  /**
   * Create an instance from an annotation data.
   */
  static newInstance (annotation) {
    let a            = new RelationAnnotation()
    a.uuid           = annotation.uuid || uuid()
    a.direction      = 'relation'
    a.rel1Annotation = AbstractAnnotation.isAnnotation(annotation.rel1) ? annotation.rel1 : window.annotationContainer.findById(annotation.rel1)
    a.rel2Annotation = AbstractAnnotation.isAnnotation(annotation.rel2) ? annotation.rel2 : window.annotationContainer.findById(annotation.rel2)
    a.text           = annotation.text
    a.color          = annotation.color
    a.readOnly       = annotation.readOnly || false
    a.zIndex         = annotation.zIndex || 10
    return a
  }

  /**
   * Create an instance from a TOML object.
   */
  static newInstanceFromTomlObject (d) {
    // d.direction = d.dir
    d.direction = 'relation'
    d.text = d.label
    let rel = RelationAnnotation.newInstance(d)
    return rel
  }

  /**
   * Set a hover event.
   */
  setHoverEvent () {
    this.$element.find('path').hover(
      this.handleHoverInEvent,
      this.handleHoverOutEvent
    )
  }

  /**
   * Setter - rel1Annotation.
   */
  set rel1Annotation (a) {
    this._rel1Annotation = a
    if (this._rel1Annotation) {
      this._rel1Annotation.on('hoverin', this.handleRelHoverIn)
      this._rel1Annotation.on('hoverout', this.handleRelHoverOut)
      this._rel1Annotation.on('rectmove', this.handleRelMove)
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
      this._rel2Annotation.on('rectmove', this.handleRelMove)
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
  render () {
    this.setStartEndPosition()
    super.render()
  }

  /**
   * Create an annotation data for save.
   */
  createAnnotation () {
    return {
      uuid      : this.uuid,
      type      : this.type,
      direction : this.direction,
      rel1      : this._rel1Annotation.uuid,
      rel2      : this._rel2Annotation.uuid,
      text      : this.text,
      color     : this.color,
      readOnly  : this.readOnly
    }
  }

  /**
   * Destroy the annotation.
   */
  destroy () {
    let promise = super.destroy()
    if (this._rel1Annotation) {
      this._rel1Annotation.removeListener('hoverin', this.handleRelHoverIn)
      this._rel1Annotation.removeListener('hoverout', this.handleRelHoverOut)
      this._rel1Annotation.removeListener('rectmove', this.handleRelMove)
      this._rel1Annotation.removeListener('delete', this.handleRelDelete)
      delete this._rel1Annotation
    }
    if (this._rel2Annotation) {
      this._rel2Annotation.removeListener('hoverin', this.handleRelHoverIn)
      this._rel2Annotation.removeListener('hoverout', this.handleRelHoverOut)
      this._rel2Annotation.removeListener('rectmove', this.handleRelMove)
      this._rel2Annotation.removeListener('delete', this.handleRelDelete)
      delete this._rel2Annotation
    }

    globalEvent.removeListener('deleteSelectedAnnotation', this.deleteSelectedAnnotation)
    globalEvent.removeListener('enableViewMode', this.enableViewMode)
    globalEvent.removeListener('rectmoveend', this.handleRelMoveEnd)

    return promise
  }

  /**
   * Delete the annotation if selected.
   */
  deleteSelectedAnnotation () {
    super.deleteSelectedAnnotation()
  }

  /**
   * Get the position for text.
   */
  // TODO No need ?
  getTextPosition () {
    this.setStartEndPosition()
    return getRelationTextPosition(this.x1, this.y1, this.x2, this.y2, this.text, this.uuid)
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
   * The callback that is called relations has been moved.
   */
  handleRelMove () {
    this.render()
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
   * The callback that is called the text content is changed.
   *
   * @param {String} newText - the content an user changed.
   */
  handleTextChanged (newText) {
    this.text = newText
    this.save()
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
   * The callback that is called at clicked.
   */
  handleClickEvent (e) {
    super.handleClickEvent(e)
    if (this.selected) {
      var data = {
        "action": "selectRelation",
        "id": this.uuid,
        "origin": this.rel1Annotation.uuid,
        "target": this.rel2Annotation.uuid
      }
      parent.Wicket.Ajax.ajax({
        "m": "POST",
        "ep": data,
        "u": window.apiUrl,
        "sh": [],
        "fh": [function () {
            alert('Something went wrong on selecting a relation annotation for: ' + data)
        }]
      });
    }
  }

  /**
   * Export Data for TOML.
   * @returns
   */
  export () {
    return {
      head  : this.rel1Annotation.exportId + '',
      tail  : this.rel2Annotation.exportId + '',
      label : this.text || ''
    }
  }

    /**
   * Export Data for TOML.
   * @returns {{type: string, dir: null, ids: *[], label: *|string}}
   */
  export040 () {
    return {
      type  : this.type,
      dir   : this.direction,
      ids   : [ this.rel1Annotation.exportId, this.rel2Annotation.exportId ],
      label : this.text || ''
    }
  }

  /**
   * Enable view mode.
   */
  enableViewMode () {

    this.disableViewMode()

    super.enableViewMode()

    if (!this.readOnly) {
      this.$element.find('path').on('click', this.handleClickEvent)
    }
  }

  /**
   * Disable view mode.
   */
  disableViewMode () {
    super.disableViewMode()
    this.$element.find('path').off('click')
  }

  /**
   * Set the start / end points of the relation.
   */
  setStartEndPosition () {
    if (this._rel1Annotation) {
      let p = this._rel1Annotation.getBoundingCirclePosition()
      this.x1 = p.x
      this.y1 = p.y
    }
    if (this._rel2Annotation) {
      let p = this._rel2Annotation.getBoundingCirclePosition()
      this.x2 = p.x
      this.y2 = p.y
    }
  }

  /**
   * @{inheritDoc}
   */
  equalTo (anno) {

    if (!anno || this.type !== anno) {
      return false
    }

    const isSame = anyOf(this.rel1Annotation.uuid, [anno.rel1Annotation.uuid, anno.rel2Annotation.uuid])
      && anyOf(this.rel2Annotation.uuid, [anno.rel1Annotation.uuid, anno.rel2Annotation.uuid])

    return isSame
  }

}
