// TODO no need this file ?
import setAttributes from '../utils/setAttributes'

/**
 * Default font size for Text.
 */
const DEFAULT_FONT_SIZE = 9.5

/**
 * Calculate boundingClientRect that is needed for rendering text.
 *
 * @param {String} text - A text to be renderd.
 * @param {SVGElement} svg - svgHTMLElement to be used for rendering text.
 * @return {Object} A boundingBox of text element.
 */
function getRect (text, svg) {
  svg.appendChild(text)
  let rect = text.getBoundingClientRect()
  text.parentNode.removeChild(text)
  return rect
}

/**
 * Create SVGTextElement from an annotation definition.
 * This is used for anntations of type `textbox`.
 *
 * @param {Object} a The annotation definition
 * @return {SVGTextElement} A text to be rendered
 */
export default function renderText (a, svg) {
  // Text.
  let text = document.createElementNS('http://www.w3.org/2000/svg', 'text')
  setAttributes(text, {
    x        : a.x,
    y        : a.y + parseInt(DEFAULT_FONT_SIZE, 10),
    fill     : a.color || '#F00',
    fontSize : DEFAULT_FONT_SIZE
  })
  text.innerHTML = a.content || a.text

  // Background.
  let box = document.createElementNS('http://www.w3.org/2000/svg', 'rect')
  let rect = getRect(text, svg)
  setAttributes(box, {
    x      : a.x - 1,
    y      : a.y,
    width  : rect.width,
    height : rect.height,
    fill   : '#FFFFFF',
    class  : 'anno-text'
  })

  // Group.
  let group = document.createElementNS('http://www.w3.org/2000/svg', 'g')
  group.classList.add('anno-text-group')
  group.setAttribute('read-only', a.readOnly === true)
  group.setAttribute('data-parent-id', a.parentId)
  group.style.visibility = 'visible'
  group.appendChild(box)
  group.appendChild(text)

  return group
}
