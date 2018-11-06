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
export function findBezierControlPoint (x1, y1, x2, y2) {

  const DISTANCE = 30

  // vertical line.
  if (x1 === x2) {
    return {
      x : x1,
      y : (y1 + y2) / 2
    }
  }

  // horizontal line.
  if (y1 === y2) {
    return {
      x : (x1 + x2) / 2,
      y : y1 - DISTANCE
    }
  }

  let center = {
    x : (x1 + x2) / 2,
    y : (y1 + y2) / 2
  }

  let gradient = (y1 - y2) / (x1 - x2)
  gradient = -1 / gradient

  let theta = Math.atan(gradient)
  let deltaX = Math.cos(theta) * DISTANCE
  let deltaY = Math.sin(theta) * DISTANCE

  if (x1 < x2) {
    // right top quadrant.
    if (y1 > y2) {
      return {
        x : center.x - Math.abs(deltaX),
        y : center.y - Math.abs(deltaY)
      }
      // right bottom quadrant.
    } else {
      return {
        x : center.x + Math.abs(deltaX),
        y : center.y - Math.abs(deltaY)
      }
    }
  } else {
    // left top quadrant.
    if (y1 > y2) {
      return {
        x : center.x + Math.abs(deltaX),
        y : center.y - Math.abs(deltaY)
      }
      // left bottom quadrant.
    } else {
      return {
        x : center.x - Math.abs(deltaX),
        y : center.y - Math.abs(deltaY)
      }
    }
  }
}

export function getRelationTextPosition (x1, y1, x2, y2, text = '', parentId = null) {

  // texts rendered.
  let rects = []
  $('.anno-text').each(function () {
    let $this = $(this)
    // Remove myself.
    if ($this.parent().data('parent-id') !== parentId) {
      rects.push({
        x      : parseFloat($this.attr('x')),
        y      : parseFloat($this.attr('y')),
        width  : parseFloat($this.attr('width')),
        height : parseFloat($this.attr('height'))
      })
    }
  })

  // Set self size.
  let myWidth = 200
  let myHeight = 15

  let addY = 5
  if (y1 < y2) {
    addY *= -1
  }

  // Find the position not overlap.
  while (true) {

    let cp = findBezierControlPoint(x1, y1, x2, y2)
    let x = x2 + (cp.x - x2) * 0.4
    let y = y2 + (cp.y - y2) * 0.4

    let ok = true
    for (let i = 0; i < rects.length; i++) {
      let r = rects[i]

      // Check rects overlap.

      let aX1 = r.x
      let aX2 = r.x + r.width
      let aY1 = r.y
      let aY2 = r.y + r.height

      let bX1 = x
      let bX2 = x + myWidth
      let bY1 = y
      let bY2 = y + myHeight

      let crossX = aX1 <= bX2 && bX1 <= aX2
      let crossY = aY1 <= bY2 && bY1 <= aY2

      if (crossX && crossY) {
        ok = false
        break
      }
    }

    if (ok) {
      return { x, y }
    }

    y1 += addY
    y2 += addY
  }
}
