import { renderSpan } from './renderSpan'
import { renderRelation } from './renderRelation'
import AbstractAnnotation from '../annotation/abstract'
import SpanAnnotation from '../annotation/span'
import RelationAnnotation from '../annotation/relation'

/**
 * Transform the rotation and scale of a node using SVG's native transform attribute.
 *
 * @param node The node to be transformed
 * @param viewport The page's viewport data
 * @return {Node}
 */
export function transform (base: HTMLElement, node: HTMLElement, viewport: any): HTMLElement {
  node.style.transform = `scale(${viewport.scale})`
  return node
}

export function appendChild (base: HTMLElement, annotation: AbstractAnnotation): HTMLElement | undefined {
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
    const viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
    const elm = transform(base, child, viewport)
    base.append(elm)
  }

  return child
}
