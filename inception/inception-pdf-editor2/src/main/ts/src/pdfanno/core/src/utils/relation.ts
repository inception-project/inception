/**
 *   The list of functionalities for a relationship between annotations.
 */

/**
 * Get bezier control point.
 *
 * @params x1 : the x of a start position.
 * @params y1 : the y of a start position.
 * @params x2 : the x of an end position.
 * @params y2 : the y of an end position.
 * @return { x, y } the position of bezier control.
 */
export function findBezierControlPoint (x1: number, y1: number, x2: number, y2: number): { x: number, y: number } {
  const DISTANCE = 30

  // vertical line.
  if (x1 === x2) {
    return {
      x: x1,
      y: (y1 + y2) / 2
    }
  }

  // horizontal line.
  if (y1 === y2) {
    return {
      x: (x1 + x2) / 2,
      y: y1 - DISTANCE
    }
  }

  const center = {
    x: (x1 + x2) / 2,
    y: (y1 + y2) / 2
  }

  let gradient = (y1 - y2) / (x1 - x2)
  gradient = -1 / gradient

  const theta = Math.atan(gradient)
  const deltaX = Math.cos(theta) * DISTANCE
  const deltaY = Math.sin(theta) * DISTANCE

  if (x1 < x2) {
    // right top quadrant.
    if (y1 > y2) {
      return {
        x: center.x - Math.abs(deltaX),
        y: center.y - Math.abs(deltaY)
      }
      // right bottom quadrant.
    } else {
      return {
        x: center.x + Math.abs(deltaX),
        y: center.y - Math.abs(deltaY)
      }
    }
  } else {
    // left top quadrant.
    if (y1 > y2) {
      return {
        x: center.x + Math.abs(deltaX),
        y: center.y - Math.abs(deltaY)
      }
      // left bottom quadrant.
    } else {
      return {
        x: center.x - Math.abs(deltaX),
        y: center.y - Math.abs(deltaY)
      }
    }
  }
}
