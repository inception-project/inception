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
function transform (base: HTMLElement, node: HTMLElement, viewport): HTMLElement {
  node.style.transform = `scale(${viewport.scale})`
  return node
}

export default function appendChild (base: HTMLElement, annotation: AbstractAnnotation): HTMLElement | undefined {
  const viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  let child: HTMLElement | undefined
  switch (annotation.type) {
    case 'span':
      child = renderSpan(annotation as SpanAnnotation)
      break
    case 'relation':
      child = renderRelation(annotation as RelationAnnotation)
      break
  }

  // If no type was provided for an annotation it will result in node being null.
  // Skip appending/transforming if node doesn't exist.
  if (child) {
    const elm = transform(base, child, viewport)
    base.append(elm)
  }

  return child
}
