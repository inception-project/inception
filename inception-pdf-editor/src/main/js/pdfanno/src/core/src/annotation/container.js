import { uuid } from 'anno-ui/src/utils'
import { ANNO_VERSION, PDFEXTRACT_VERSION } from '../version'
import { toTomlString, fromTomlString } from '../utils/tomlString'
import { dispatchWindowEvent } from '../utils/event'
// import { convertToExportY } from '../../../shared/coords'
import SpanAnnotation from './span'
import RectAnnotation from './rect'
import RelationAnnotation from './relation'
import semver from 'semver'
import Ajv from 'ajv'
/**
 * Annotation Container.
 */
export default class AnnotationContainer {

  /**
   * Constructor.
   */
  constructor () {
    this.set = new Set()
    this.ajv = new Ajv({
      allErrors : true
    })
    this.validate = this.ajv.compile(require('../../../../schemas/pdfanno-schema.json'))
  }

  /**
   * Add an annotation to the container.
   */
  add (annotation) {
    this.set.add(annotation)
    dispatchWindowEvent('annotationUpdated')
  }

  /**
   * Remove the annotation from the container.
   */
  remove (annotation) {
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
  getAllAnnotations () {
    let list = []
    this.set.forEach(a => list.push(a))
    return list
  }

  /**
   * Get annotations which user select.
   */
  getSelectedAnnotations () {
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

  /**
   * Change the annotations color, if the text is the same in an annotation.
   *
   * annoType : span, one-way, two-way, link
   */
  changeColor ({ text, color, uuid, annoType }) {
    console.log('changeColor: ', text, color, uuid)
    if (uuid) {
      const a = this.findById(uuid)
      if (a) {
        a.color = color
        a.render()
        a.enableViewMode()
      }
    } else {
      this.getAllAnnotations()
        .filter(a => a.text === text)
        .filter(a => {
          if (annoType === 'span') {
            return a.type === annoType
          } else if (annoType === 'relation') {
            if (a.type === 'relation' && a.direction === annoType) {
              return true
            }
          }
          return false
        }).forEach(a => {
          a.color = color
          a.render()
          a.enableViewMode()
        })
    }
  }

  setColor (colorMap) {
    console.log('setColor:', colorMap)
    Object.keys(colorMap).forEach(annoType => {
      if (annoType === 'default') {
        return
      }
      Object.keys(colorMap[annoType]).forEach(text => {
        const color = colorMap[annoType][text]
        this.changeColor({ text, color, annoType })
      })
    })
  }

  /**
   * Export annotations as a TOML string.
   */
  exportData ({exportType = 'toml'} = {}) {

    return new Promise((resolve, reject) => {

      let dataExport = {}

      // Set version.
      dataExport.pdfanno = ANNO_VERSION
      dataExport.pdfextract = PDFEXTRACT_VERSION

      // Only writable.
      const annos = this.getAllAnnotations().filter(a => !a.readOnly)

      // Sort by create time.
      // This reason is that a relation need start/end annotation ids which are numbered at export.
      annos.sort((a1, a2) => a1.createdAt - a2.createdAt)

      // The ID for specifing an annotation on a TOML file.
      // This ID is sequential.
      let id = 0

      // Create export data.
      annos.forEach(annotation => {

        // Increment to next.
        id++

        // Span.
        if (annotation.type === 'span') {
          if (!dataExport['spans']) {
            dataExport['spans'] = []
          }
          dataExport['spans'].push(annotation.export(id))
          // Save temporary for relation.
          annotation.exportId = id

        // Relation.
        } else if (annotation.type === 'relation') {
          if (!dataExport['relations']) {
            dataExport['relations'] = []
          }
          dataExport['relations'].push(annotation.export())
        }
      })

      // Remove exportId.
      annos.forEach(annotation => {
        delete annotation.exportId
      })

      // schema Validation
      if (!this.validate(dataExport)) {
        // errorをcatchしづらい
        // reject(this.validate.errors)
        // return
        console.error(JSON.stringify(this.validate.errors))
      }

      if (exportType === 'json') {
        resolve(dataExport)
      } else {
        resolve(toTomlString(dataExport))
      }
    })
  }

  _findSpan (tomlObject, id) {
    return tomlObject.spans.find(v => {
      return id === v.id
    })
  }

  /**
   * Import annotations.
   */
  importAnnotations (data, isPrimary) {

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
        let tomlObject = fromTomlString(tomlString)
        if (!tomlObject) {
          return
        }

        let pdfannoVersion = tomlObject.pdfanno || tomlObject.version

        if (semver.gt(pdfannoVersion, '0.4.0')) {
          // schema Validation
          if (!this.validate(tomlObject)) {
            reject(this.validate.errors)
            return
          }
          this.importAnnotations041(tomlObject, i, readOnly, getColor)
        } else {
          this.importAnnotations040(tomlObject, i, readOnly, getColor)
        }
      })

      // Done.
      resolve(true)
    })
  }

  /**
   * Import annotations.
   */
  importAnnotations040 (tomlObject, tomlIndex, readOnly, getColor) {

    for (const key in tomlObject) {

      let d = tomlObject[key]

      // Skip if the content is not object, like version string.
      if (typeof d !== 'object') {
        continue
      }

      d.uuid = uuid()
      d.readOnly = readOnly

      if (d.type === 'span') {

        let span = SpanAnnotation.newInstanceFromTomlObject(d)
        span.color = getColor(tomlIndex, span.type, span.text)
        span.save()
        span.render()
        span.enableViewMode()

        // Rect.
      } else if (d.type === 'rect') {

        let rect = RectAnnotation.newInstanceFromTomlObject(d)
        rect.color = getColor(tomlIndex, rect.type, rect.text)
        rect.save()
        rect.render()
        rect.enableViewMode()

        // Relation.
      } else if (d.type === 'relation') {

        d.rel1 = tomlObject[d.ids[0]].uuid
        d.rel2 = tomlObject[d.ids[1]].uuid
        let relation = RelationAnnotation.newInstanceFromTomlObject(d)
        relation.color = getColor(tomlIndex, relation.direction, relation.text)
        relation.save()
        relation.render()
        relation.enableViewMode()

      } else {
        console.log('Unknown: ', key, d)
      }
    }
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
          obj.uuid = obj.id || uuid()
          obj.readOnly = readOnly

          if (key === 'spans') {
            const span = SpanAnnotation.newInstanceFromTomlObject(obj)
// BEGIN INCEpTION EXTENSION - #981 - Labels for Annotations in PDF editor
/*
            span.color = getColor(tomlIndex, 'span', span.text)
*/
// END INCEpTION EXTENSION
            span.save()
            span.render()
            span.enableViewMode()

          } else if (key === 'relations') {
            const span1 = this._findSpan(tomlObject, obj.head)
            const span2 = this._findSpan(tomlObject, obj.tail)
            obj.rel1 = span1 ? span1.uuid : null
            obj.rel2 = span2 ? span2.uuid : null
            const relation = RelationAnnotation.newInstanceFromTomlObject(obj)
// BEGIN INCEpTION EXTENSION - #981 - Labels for Annotations in PDF editor
/*
            relation.color = getColor(tomlIndex, relation.direction, relation.text)
*/
// END INCEpTION EXTENSION
            relation.save()
            relation.render()
            relation.enableViewMode()

          }
        })
      }
    })
  }
}
