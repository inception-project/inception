import * as textInput from '../utils/textInput'
import RelationAnnotation from '../annotation/relation'
import { scaleDown } from './utils'

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

  let annotation = new RelationAnnotation()
  annotation.direction = type
  annotation.rel1Annotation = anno1
  annotation.rel2Annotation = anno2
  annotation.text = text
  annotation.color = color

  annotation.save()
  annotation.render()

  // TODO Refactoring.
  // TODO 適切な場所に移動する.
  // Deselect all.
  window.annotationContainer
    .getSelectedAnnotations()
    .forEach(a => a.deselect())

  // Select.
  annotation.select()

  // New type text.
  textInput.enable({ uuid : annotation.uuid, autoFocus : true, text })

  return annotation
}

window.addEventListener('DOMContentLoaded', () => {

  const $viewer = $('#viewer')

  window.addEventListener('annotationHoverIn', (e) => {
    hoveredAnnotation = e.detail
  })

  window.addEventListener('annotationHoverOut', () => {
    hoveredAnnotation = null
  })

  $viewer.on('mousedown', '.anno-knob', () => {
    mousedownFired = true
    if (hoveredAnnotation) {
      onAnnotation = true
      relationAnnotation = new RelationAnnotation()
      relationAnnotation.rel1Annotation = hoveredAnnotation
      relationAnnotation.direction = 'relation'
      relationAnnotation.readOnly = false
    }
  })

  $viewer.on('mousemove', (e) => {
    if (mousedownFired && onAnnotation) {
      drawing = true
      let p = scaleDown(getClientXY(e))
      relationAnnotation.x2 = p.x
      relationAnnotation.y2 = p.y
      relationAnnotation.render()
    }
  })

  $viewer.on('mouseup', () => {
    if (mousedownFired && onAnnotation && drawing) {
      var data = {
        "action": "createRelation",
        "origin": relationAnnotation.rel1Annotation.uuid,
        "target": hoveredAnnotation ? hoveredAnnotation.uuid : -1,
      }
      parent.Wicket.Ajax.ajax({
        "m": "POST",
        "ep": data,
        "u": window.apiUrl,
        "sh": [],
        "fh": [function () {
            alert('Something went wrong on creating new relation for: ' + data)
        }]
      });
    }
    drawing = false
    onAnnotation = false
    mousedownFired = false
    relationAnnotation = null
  })
})

function getClientXY(e) {
  let rect = document.getElementById('annoLayer').getBoundingClientRect()
  let x = e.clientX - rect.left
  let y = e.clientY - rect.top
  return {x, y}
}

let relationAnnotation = null
let hoveredAnnotation = null
let mousedownFired = false
let onAnnotation = false
let drawing = false
