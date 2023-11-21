import { fromTomlString } from '../utils/tomlString'
import { dispatchWindowEvent } from '../../../shared/util'
import SpanAnnotation from './span'
import RelationAnnotation from './relation.js'
import Ajv, { ValidateFunction } from 'ajv'
import AbstractAnnotation from './abstract'

/**
 * Annotation Container.
 */
export default class AnnotationContainer {
  set = new Set<AbstractAnnotation>()
  ajv = new Ajv({ allErrors: true })
  validate : ValidateFunction

  /**
   * Constructor.
   */
  constructor () {
    this.validate = this.ajv.compile(require('../../../schemas/pdfanno-schema.json'))
  }

  /**
   * Add an annotation to the container.
   */
  add (annotation: AbstractAnnotation) {
    this.set.add(annotation)
    dispatchWindowEvent('annotationUpdated')
  }

  /**
   * Remove the annotation from the container.
   */
  remove (annotation: AbstractAnnotation) {
    this.set.delete(annotation)
    dispatchWindowEvent('annotationUpdated')
  }

  /**
   * Remove all annotations.
   */
  destroy () {
    console.log('AnnotationContainer#destroy')
    this.set.forEach(a => a.destroy())
    this.set = new Set()
  }

  /**
   * Get all annotations from the container.
   */
  getAllAnnotations () : AbstractAnnotation[] {
    const list = []
    this.set.forEach(a => list.push(a))
    return list
  }

  /**
   * Get annotations which user select.
   */
  getSelectedAnnotations () : AbstractAnnotation[] {
    return this.getAllAnnotations().filter(a => a.selected)
  }

  /**
   * Find an annotation by the id which an annotation has.
   */
  findById (uuid) {
    uuid = String(uuid) // `uuid` must be string.
    let annotation = null
    this.set.forEach(a => {
      if (a.uuid === uuid) {
        annotation = a
      }
    })
    return annotation
  }

  _findSpan (tomlObject, id) {
    return tomlObject.spans.find(v => {
      return id === v.id
    })
  }

  /**
   * Import annotations.
   */
  importAnnotations (data, isPrimary: boolean) {
    const readOnly = !isPrimary
    const colorMap = data.colorMap

    function getColor (index, type, text) {
      let color = colorMap.default
      if (colorMap[type] && colorMap[type][text]) {
        color = colorMap[type][text]
      }
      return color
    }

    return new Promise((resolve, reject) => {
      // Delete old ones.
      this.getAllAnnotations()
        .filter(a => a.readOnly === readOnly)
        .forEach(a => a.destroy())

      // Add annotations.
      data.annotations.forEach((tomlString, i) => {
        // Create a object from TOML string.
        const tomlObject = fromTomlString(tomlString)
        if (!tomlObject) {
          return
        }

        // schema Validation
        if (!this.validate(tomlObject)) {
          reject(this.validate.errors)
          return
        }
        this.importAnnotations041(tomlObject, i, readOnly, getColor)
      })

      // Done.
      resolve(true)
    })
  }

  /**
   * Import annotations.
   */
  importAnnotations041 (tomlObject, tomlIndex, readOnly, getColor) {
    // order is important.
    ;['spans', 'relations'].forEach(key => {
      const objs = tomlObject[key]
      if (Array.isArray(objs)) {
        objs.forEach(obj => {
          obj.uuid = obj.id
          obj.readOnly = readOnly

          if (key === 'spans') {
            const span = SpanAnnotation.newInstanceFromTomlObject(obj)
            span.save()
            span.render()
            span.enableViewMode()
          } else if (key === 'relations') {
            const span1 = this._findSpan(tomlObject, obj.head)
            const span2 = this._findSpan(tomlObject, obj.tail)
            obj.rel1 = span1 ? span1.uuid : null
            obj.rel2 = span2 ? span2.uuid : null
            const relation = RelationAnnotation.newInstanceFromTomlObject(obj)
            relation.save()
            relation.render()
            relation.enableViewMode()
          }
        })
      }
    })
  }
}
