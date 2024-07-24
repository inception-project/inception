import { renderSpan } from './renderSpan'
import { renderRelation } from './renderRelation'
import AbstractAnnotation from '../model/AbstractAnnotation'
import SpanAnnotation from '../model/SpanAnnotation'
import RelationAnnotation from '../model/RelationAnnotation'

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

export function appendChild (base: HTMLElement, annotation: AbstractAnnotation): HTMLElement | null {
  let child: HTMLElement | null
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
    const viewport = globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport
    const elm = transform(base, child, viewport)
    base.append(elm)
  }

  return child
}
