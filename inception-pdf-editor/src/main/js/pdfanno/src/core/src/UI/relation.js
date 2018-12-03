import * as textInput from '../utils/textInput'
import RelationAnnotation from '../annotation/relation'

/**
 * Create a new Relation annotation.
 */
export function createRelation ({ type, anno1, anno2, text, color }) {
  // TODO No need?
  // for old style.
  if (arguments.length === 3) {
    type = arguments[0]
    anno1 = arguments[1]
    anno2 = arguments[2]
  }

  let annotation = new RelationAnnotation()
  annotation.direction = type
  annotation.rel1Annotation = anno1
  annotation.rel2Annotation = anno2
  annotation.text = text
  annotation.color = color

  annotation.save()
  annotation.render()

  // TODO Refactoring.
  // TODO 適切な場所に移動する.
  // Deselect all.
  window.annotationContainer
    .getSelectedAnnotations()
    .forEach(a => a.deselect())

  // Select.
  annotation.select()

  // New type text.
  textInput.enable({ uuid : annotation.uuid, autoFocus : true, text })

  return annotation
}
