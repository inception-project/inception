import RelationAnnotation from '../annotation/relation.js'
import { scaleDown } from './utils'

let relationAnnotation = null
let hoveredAnnotation = null
let mousedownFired = false
let onAnnotation = false
let drawing = false

/**
 * Create a new Relation annotation.
 */
export function createRelation ({ type, anno1, anno2, text, color }) {
  // TODO No need?
  // for old style.
  if (arguments.length === 3) {
    type = arguments[0]
    anno1 = arguments[1]
    anno2 = arguments[2]
  }

  const annotation = new RelationAnnotation()
  annotation.direction = type
  annotation.rel1Annotation = anno1
  annotation.rel2Annotation = anno2
  annotation.text = text
  annotation.color = color

  annotation.save()
  annotation.render()

  // TODO Refactoring.
  // Deselect all.
  window.annotationContainer
    .getSelectedAnnotations()
    .forEach(a => a.deselect())

  // Select.
  annotation.select()

  return annotation
}

export function installRelationSelection () {
  const viewer = document.getElementById('viewer')

  window.addEventListener('annotationHoverIn', (e) => {
    hoveredAnnotation = e.detail
  })

  window.addEventListener('annotationHoverOut', () => {
    hoveredAnnotation = null
  })

  viewer.addEventListener('mousedown', ev => {
    if (!(ev.target as HTMLElement).closest('.anno-knob')) {
      return
    }

    mousedownFired = true
    if (hoveredAnnotation) {
      onAnnotation = true
      relationAnnotation = new RelationAnnotation()
      relationAnnotation.rel1Annotation = hoveredAnnotation
      relationAnnotation.direction = 'relation'
      relationAnnotation.readOnly = false
    }
  })

  viewer.addEventListener('mousemove', (e) => {
    if (mousedownFired && onAnnotation) {
      drawing = true
      const p = scaleDown(getClientXY(e))
      relationAnnotation.x2 = p.x
      relationAnnotation.y2 = p.y
      relationAnnotation.render()
    }
  })

  viewer.addEventListener('mouseup', () => {
    if (mousedownFired && onAnnotation && drawing && hoveredAnnotation) {
      const event = new CustomEvent('createRelationAnnotation', {
        bubbles: true,
        detail: {
          origin: relationAnnotation.rel1Annotation.uuid,
          target: hoveredAnnotation.uuid
        }
      })
      viewer.dispatchEvent(event)
    }
    if (relationAnnotation) {
      relationAnnotation.destroy()
    }
    drawing = false
    onAnnotation = false
    mousedownFired = false
    relationAnnotation = null
  })
}

function getClientXY (e) {
  const rect = document.getElementById('annoLayer').getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  return { x, y }
}
