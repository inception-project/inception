import { renderSpan } from './renderSpan'
import { renderRelation } from './renderRelation'
import AbstractAnnotation from '../annotation/abstract'
import SpanAnnotation from '../annotation/span'
import RelationAnnotation from '../annotation/relation'

/**
 * Transform the rotation and scale of a node using SVG's native transform attribute.
 *
 * @param {Node} node The node to be transformed
 * @param {Object} viewport The page's viewport data
 * @return {Node}
 */
function transform (node: HTMLElement, viewport): HTMLElement {
  node.style.transform = `scale(${viewport.scale})`
  return node
}

/**
 * Append an annotation as a child of an SVG.
 *
 * @param {SVGElement} svg The SVG element to append the annotation to
 * @param {Object} annotation The annotation definition to render and append
 * @param {Object} viewport The page's viewport data
 * @return {SVGElement} A node that was created and appended by this function
 */
export default function appendChild (svg, annotation: AbstractAnnotation, viewport?): HTMLElement {
  // TODO no need third argument(viewport) ?
  if (!viewport) {
    viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  }

  let child: HTMLElement
  switch (annotation.type) {
    case 'span':
      child = renderSpan(annotation as SpanAnnotation, svg)
      break
    case 'relation':
      child = renderRelation(annotation as RelationAnnotation, svg)
      break
  }

  // If no type was provided for an annotation it will result in node being null.
  // Skip appending/transforming if node doesn't exist.
  if (child) {
    const elm = transform(child, viewport)

    svg.append(elm)
  }
  return child
}
