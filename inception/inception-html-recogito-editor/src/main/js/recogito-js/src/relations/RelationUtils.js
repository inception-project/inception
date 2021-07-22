/** 
 * Returns the 'graph node' ({ annotation: ..., elements: ...}) for the
 * given annotation ID.
 */
export const getNodeById = function(contentEl, annotationId) {
  const elements = contentEl.querySelectorAll(`*[data-id="${annotationId}"]`);
  return (elements.length > 0) ? 
    { annotation: elements[0].annotation, elements: Array.from(elements) } : null;
};

/**
 * Returns the graph node for the target of the given mouse event (or
 * null if the event target is not an annotation span).
 */
export const getNodeForEvent = function(evt) {
  // Sorts annotations by length, so we can reliably get the inner-most
  const sortByQuoteLengthDesc = annotations => 
    annotations.sort((a, b) => a.quote.length - b.quote.length);

  const annotationSpan = evt.target.closest('.r6o-annotation');

  if (annotationSpan) {
    // All stacked annotation spans
    const spans = getAnnotationSpansRecursive(annotationSpan);

    // Annotation from the inner-most span in the stack
    const annotation = sortByQuoteLengthDesc(spans.map(span => span.annotation))[0];

    // ALL spans for this annotation (not just the hovered one)
    const elements = document.querySelectorAll(`.r6o-annotation[data-id="${annotation.id}"]`);
    
    return { annotation, elements: Array.from(elements) };    
  }
}

/**
 * Helper: gets all stacked annotation SPANS for an element.
 *
 * Reminder - annotations can be nested. This helper retrieves the whole stack.
 *
 * <span class="annotation" data-id="annotation-01">
 *   <span class="annotation" data-id="annotation-02">
 *     <span class="annotation" data-id="annotation-03">foo</span>
 *   </span>
 * </span>
 */
export const getAnnotationSpansRecursive = function(element, s) {
  const spans = s ? s : [ ];
  spans.push(element);

  const parent = element.parentNode;

  return parent.classList.contains('r6o-annotation') ?
    getAnnotationSpansRecursive(parent, spans) : spans;
}