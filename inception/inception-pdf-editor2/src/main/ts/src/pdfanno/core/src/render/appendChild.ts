import { renderSpan } from './renderSpan'
import { renderRelation } from './renderRelation'
import AbstractAnnotation from '../model/AbstractAnnotation'
import SpanAnnotation from '../model/SpanAnnotation'
import RelationAnnotation from '../model/RelationAnnotation'

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
    const elm = annotation.transform(base, child, viewport)
    base.append(elm)
  }

  return child
}
