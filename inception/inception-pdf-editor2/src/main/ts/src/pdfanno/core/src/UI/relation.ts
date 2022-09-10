import { annoLayer2Id } from '../../../pdfanno.js'
import RelationAnnotation from '../annotation/relation.js'
import SpanAnnotation from '../annotation/span.js'
import { scaleDown } from './utils'

let relationAnnotation: RelationAnnotation | null = null
let hoveredAnnotation: SpanAnnotation | null = null
let mousedownFired = false
let onAnnotation = false
let drawing = false
let dragStartedAt: MouseEvent | null = null

export function installRelationSelection () {
  const viewer = document.getElementById('viewer')
  if (!viewer) {
    console.error('Viewer element not found')
    return
  }

  window.addEventListener('annotationHoverIn', (e) => {
    hoveredAnnotation = e.detail
  })

  window.addEventListener('annotationHoverOut', () => {
    hoveredAnnotation = null
  })

  viewer.addEventListener('mousedown', ev => handleMouseDown(ev))
  viewer.addEventListener('mousemove', ev => handleMouseMove(ev))
  viewer.addEventListener('mouseup', ev => handleMouseUp(ev))
}

function handleMouseDown (ev: MouseEvent): void {
  if (!(ev.target as HTMLElement).closest('.anno-knob')) {
    return
  }

  mousedownFired = true
  if (hoveredAnnotation) {
    onAnnotation = true
    relationAnnotation = new RelationAnnotation()
    relationAnnotation.rel1Annotation = hoveredAnnotation
    relationAnnotation.readOnly = false
    dragStartedAt = ev
  }
}

function handleMouseUp (ev: MouseEvent): void {
  if (mousedownFired && onAnnotation && drawing && hoveredAnnotation && relationAnnotation) {
    const event = new CustomEvent('createRelationAnnotation', {
      bubbles: true,
      detail: {
        origin: relationAnnotation.rel1Annotation.vid,
        target: hoveredAnnotation.vid
      }
    })
    document.getElementById('viewer')?.dispatchEvent(event)
  }
  if (relationAnnotation) {
    relationAnnotation.destroy()
  }
  drawing = false
  onAnnotation = false
  mousedownFired = false
  relationAnnotation = null
  dragStartedAt = null
}

function handleMouseMove (ev: MouseEvent): void {
  if (mousedownFired && onAnnotation && relationAnnotation) {
    if (!drawing && dragStartedAt) {
      const deltaX = Math.abs(dragStartedAt.pageX - ev.pageX)
      const deltaY = Math.abs(dragStartedAt.pageY - ev.pageY)

      if (deltaX > 5 || deltaY > 5) {
        drawing = true
      } else {
        return
      }
    }

    const p = scaleDown(getClientXY(ev))
    relationAnnotation.x2 = p.x
    relationAnnotation.y2 = p.y
    relationAnnotation.render()
  }
}

function getClientXY (e: MouseEvent): { x: number, y: number } {
  const rect = document.getElementById(annoLayer2Id).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  return { x, y }
}
