/**
 * Change color definition style from hex to rgba.
 */
export function hex2rgba (hex, alpha = 1) {
  // long version
  let r = hex.match(/^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i)
  let c = null
  if (r) {
    c = r.slice(1, 4).map(function (x) { return parseInt(x, 16) })
  }
  // short version
  r = hex.match(/^#([0-9a-f])([0-9a-f])([0-9a-f])$/i)
  if (r) {
    c = r.slice(1, 4).map(function (x) { return 0x11 * parseInt(x, 16) })
  }
  if (!c) {
    return hex
  }
  return `rgba(${c[0]}, ${c[1]}, ${c[2]}, ${alpha})`
}
