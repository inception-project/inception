import toml from 'toml'

/**
 * Expose public APIs.
 */
export function expose () {
  window.add = addAnnotation
  window.delete = deleteAnnotation
  window.SpanAnnotation = PublicSpanAnnotation
  window.RelationAnnotation = PublicRelationAnnotation
  window.readTOML = readTOML
  window.clear = clear
}

/**
 * Add an annotation, and render it.
 */
export function addAnnotation (publicAnnotation) {

  let a = publicAnnotation.annotation
  window.annoPage.addAnnotation(a)
  a.render()
  a.enableViewMode()
  a.save()
}

/**
 * Delete an annotation, and also detach it from view.
 */
export function deleteAnnotation (publicAnnotation) {

  publicAnnotation.annotation.destroy()
}

/**
 * Rect Annotation Class wrapping the core.
 */
export class PublicSpanAnnotation {

  constructor ({ page, position, label = '', text = '', id = 0, uuid = '', zIndex = 10, textrange = [], color = null }) {

    // Check inputs.
    if (!page || typeof page !== 'number') {
      throw new Error('Set the page as number.')
    }

    let span = window.annoPage.createSpanAnnotation({
      uuid,
      page,
      text         : label,
      color        : color || '#FFEB3B',
      readOnly     : false,
      selectedText : text,
      textRange    : textrange,
      zIndex
    })

    this.annotation = span
  }
}

/**
 * Rect Annotation Class wrapping the core.
 */
export class PublicRelationAnnotation {

  constructor ({ dir, ids, label = '', id = 0, uuid = '', color = '#FF0000' }) {

    // Check inputs.
    if (!dir) {
      throw new Error('Set the dir.')
    }
    if (!ids || ids.length !== 2) {
      throw new Error('Set the ids.')
    }

    let r = window.annoPage.createRelationAnnotation({
      uuid,
      direction : dir,
      rel1      : typeof ids[0] === 'object' ? ids[0].annotation : ids[0],
      rel2      : typeof ids[1] === 'object' ? ids[1].annotation : ids[1],
      text      : label,
      color,
      readOnly  : false
    })

    this.annotation = r
  }
}

/**
 * TOML parser.
 */
export const readTOML = toml.parse

/**
 * Delete all annotations.
 */
export function clear () {
  window.annoPage.clearAllAnnotations()
}
