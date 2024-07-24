/**
 * Circle radius.
 */
export const DEFAULT_RADIUS = 7

/**
 * Create a bounding circle.
 * @param {Object} the data for rendering.
 */
export function renderKnob ({ a, x, y, readOnly, text }): HTMLElement {
  // Adjust the position.
  [x, y] = adjustPoint(x, (y - (DEFAULT_RADIUS + 2)), DEFAULT_RADIUS)

  const knob = document.createElement('div')
  knob.classList.add('anno-knob')
  a.classList.forEach(c => knob.classList.add(c))
  if (readOnly) {
    knob.classList.add('is-readonly')
  }
  knob.style.top = `${y}px`
  knob.style.left = `${x}px`
  knob.style.width = DEFAULT_RADIUS + 'px'
  knob.style.height = DEFAULT_RADIUS + 'px'
  if (a.color) {
    knob.style.backgroundColor = a.color
  }
  return knob
}

/**
 * Adjust the circle position not overlay another.
 */
function adjustPoint (x, y, radius) {
  // Get all knobs.
  const circles = document.querySelectorAll('.anno-knob') as NodeListOf<HTMLElement>

  // Find a position where all knobs are not placed at.
  while (true) {
    let good = true
    circles.forEach(knob => {
      const x1 = parseInt(knob.style.left)
      const y1 = parseInt(knob.style.top)
      const distance1 = Math.pow(x - x1, 2) + Math.pow(y - y1, 2)
      const distance2 = Math.pow(radius, 2)
      if (distance1 < distance2) {
        good = false
      }
    })
    if (good) {
      break
    }
    y -= 1
  }
  return [x, y]
}
