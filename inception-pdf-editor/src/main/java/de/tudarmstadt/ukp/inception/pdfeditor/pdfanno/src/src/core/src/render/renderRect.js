import { renderKnob } from './renderKnob'
import { hex2rgba } from '../utils/color'

/**
 * Create a rect annotation.
 * @param {RectAnnotation} a - rect annotation.
 */
export function renderRect (a) {

  let color = a.color || '#FF0'

  const $base = $('<div class="anno-rect-base"/>')

  $base.append($('<div class="anno-rect"/>').css({
    top             : `${a.y}px`,
    left            : `${a.x}px`,
    width           : `${a.width}px`,
    height          : `${a.height}px`,
    border          : `1px solid ${color}`,
    backgroundColor : a.readOnly ? 'none' : hex2rgba(color, 0.3)
  }))

  $base.append(renderKnob(a))

  return $base[0]
}
