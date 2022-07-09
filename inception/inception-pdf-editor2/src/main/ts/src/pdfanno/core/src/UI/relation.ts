import { annoLayer2Id } from '../../../pdfanno.js'
import RelationAnnotation from '../annotation/relation.js'
import SpanAnnotation from '../annotation/span.js'
import { scaleDown } from './utils'

let relationAnnotation : RelationAnnotation = null
let hoveredAnnotation : SpanAnnotation = null
let mousedownFired = false
let onAnnotation = false
let drawing = false

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
          origin: relationAnnotation.rel1Annotation.vid,
          target: hoveredAnnotation.vid
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

function getClientXY (e: MouseEvent) {
  const rect = document.getElementById(annoLayer2Id).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  return { x, y }
}
