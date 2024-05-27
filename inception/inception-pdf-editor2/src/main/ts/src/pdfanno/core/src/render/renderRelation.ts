import setAttributes from '../utils/setAttributes'
import { DEFAULT_RADIUS } from './renderKnob'
import { findBezierControlPoint } from '../utils/relation.js'
import RelationAnnotation from '../model/RelationAnnotation'

/**
 * Create a RELATION annotation.
 *
 * @param a The annotation definition
 * @return A group of a relation to be rendered
 */
export function renderRelation (a: RelationAnnotation): HTMLDivElement {
  a.color = a.color || '#F00'

  // Adjust the start/end points.
  const xDiff = a.x1 - a.x2
  const yDiff = a.y1 - a.y2

  // if difference of x and difference of y is both 0 use 0 for atan
  const theta = Math.atan(xDiff === 0 && yDiff === 0 ? 0 : (yDiff / xDiff))
  const sign = (a.x1 < a.x2 ? 1 : -1)
  a.x1 += DEFAULT_RADIUS * Math.cos(theta) * sign
  a.x2 -= DEFAULT_RADIUS * Math.cos(theta) * sign
  a.y1 += DEFAULT_RADIUS * Math.sin(theta) * sign
  a.y2 -= DEFAULT_RADIUS * Math.sin(theta) * sign

  const top = Math.min(a.y1, a.y2)
  const left = Math.min(a.x1, a.x2)
  const width = Math.abs(a.x1 - a.x2)
  const height = Math.abs(a.y1 - a.y2)

  const [svgElement, margin] = createSVGElement(top, left, width, height)

  // Transform coords.
  a.x1 = a.x1 - left + margin
  a.x2 = a.x2 - left + margin
  a.y1 = a.y1 - top + margin
  a.y2 = a.y2 - top + margin

  // <svg viewBox="0 0 200 200">
  //     <marker id="m_ar" viewBox="0 0 10 10" refX="5" refY="5" markerUnits="strokeWidth" preserveAspectRatio="none" markerWidth="2" markerHeight="3" orient="auto-start-reverse">
  //         <polygon points="0,0 0,10 10,5" fill="red" id="ms"/>
  //     </marker>
  //     <path d="M50,50 h100" fill="none" stroke="black" stroke-width="10" marker-start="url(#m_ar)" marker-end="url(#m_ar)"/>
  // </svg>

  const group = document.createElementNS('http://www.w3.org/2000/svg', 'g')
  setAttributes(group, {
    fill: a.color,
    stroke: a.color
  })
  group.style.visibility = 'visible'
  group.style.pointerEvents = 'auto'
  group.setAttribute('read-only', a.readOnly === true ? 'true' : null)

  svgElement.appendChild(group)

  const markerId = 'relationhead' + a.color.replace('#', '')

  if (!document.querySelector('#' + markerId)) {
    const marker = document.createElementNS('http://www.w3.org/2000/svg', 'marker')
    setAttributes(marker, {
      viewBox: '0 0 10 10',
      fill: a.color,
      id: markerId,
      orient: 'auto-start-reverse'
    })
    marker.setAttribute('preserveAspectRatio', 'none')
    marker.setAttribute('markerWidth', '5')
    marker.setAttribute('markerHeight', '5')
    marker.setAttribute('refX', '5')
    marker.setAttribute('refY', '5')
    group.appendChild(marker)

    const polygon = document.createElementNS('http://www.w3.org/2000/svg', 'polygon')
    setAttributes(polygon, {
      points: '0,0 0,10 10,5'
    })
    marker.appendChild(polygon)
  }

  // Find Control points.
  const control = findBezierControlPoint(a.x1, a.y1, a.x2, a.y2)

  // Create Outline.
  const outline = document.createElementNS('http://www.w3.org/2000/svg', 'path')
  setAttributes(outline, {
    d: `M ${a.x1} ${a.y1} Q ${control.x} ${control.y} ${a.x2} ${a.y2}`,
    class: 'anno-relation-outline'
  })
  group.appendChild(outline)

  /*
   * <path d="M 25 25 Q 175 25 175 175" stroke="blue" fill="none"/>
   */
  const classlist = ['anno-relation']
  a?.classList.forEach(c => classlist.push(c))

  const relation = document.createElementNS('http://www.w3.org/2000/svg', 'path')
  setAttributes(relation, {
    d: `M ${a.x1} ${a.y1} Q ${control.x} ${control.y} ${a.x2} ${a.y2}`,
    stroke: a.color,
    strokeWidth: 1,
    fill: 'none',
    class: classlist.join(' ')
  })

  // Triangle for the end point.
  relation.setAttribute('marker-end', `url(#${markerId})`)

  group.appendChild(relation)

  const base = document.createElement('div')
  base.style.position = 'absolute'
  base.style.top = '0'
  base.style.left = '0'
  base.style.visibility = 'visible'
  base.classList.add('anno-relation')
  base.appendChild(svgElement)

  return base
}

function createSVGElement (top, left, width, height): [SVGElement, number] {
  // the margin for rendering an arrow curve.
  const margin = 50

  // Add an annotation layer.
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  svg.style.position = 'absolute'
  svg.style.top = `${top - margin}px`
  svg.style.left = `${left - margin}px`
  svg.style.width = `${width + margin * 2}px`
  svg.style.height = `${height + margin * 2}px`
  svg.style.pointerEvents = 'none'
  svg.style.zIndex = '2'
  svg.setAttribute('x', '0')
  svg.setAttribute('y', '0')

  return [svg, margin]
}
