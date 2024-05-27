import RelationAnnotation from '../model/RelationAnnotation.js'
import SpanAnnotation from '../model/SpanAnnotation.js'
import { getClientXY, scaleDown } from './utils'

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

  window.addEventListener('annotationHoverIn', ev => handleHoverIn(ev))
  window.addEventListener('annotationHoverOut', ev => handleHoverOut(ev))

  viewer.addEventListener('mousedown', ev => handleMouseDown(ev))
  viewer.addEventListener('mousemove', ev => handleMouseMove(ev))
  viewer.addEventListener('mouseup', ev => handleMouseUp(ev))
}

function handleHoverOut (ev: Event) {
  hoveredAnnotation = null
}

function handleHoverIn (ev: Event) {
  if (ev instanceof CustomEvent) {
    hoveredAnnotation = ev.detail
  }
}

function handleMouseDown (ev: MouseEvent): void {
  if (!(ev.target as HTMLElement).closest('.anno-knob')) return

  mousedownFired = true

  if (!hoveredAnnotation) return

  if (`${hoveredAnnotation.vid}`.includes(':')) return

  onAnnotation = true
  relationAnnotation = new RelationAnnotation()
  relationAnnotation.rel1Annotation = hoveredAnnotation
  relationAnnotation.readOnly = false
  dragStartedAt = ev
}

function handleMouseUp (ev: MouseEvent): void {
  if (mousedownFired && onAnnotation && drawing && hoveredAnnotation && relationAnnotation?.rel1Annotation) {
    const event = new CustomEvent('createRelationAnnotation', {
      bubbles: true,
      detail: {
        originalEvent: ev,
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
