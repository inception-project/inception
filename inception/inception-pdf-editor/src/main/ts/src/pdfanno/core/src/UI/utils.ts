import { Rect } from '../../../page/textLayer'

export const BORDER_COLOR = '#00BFFF'

/**
 * Adjust scale from normalized scale (100%) to rendered scale.
 *
 * @param {SVGElement} svg The SVG to gather metadata from
 * @param {Object} rect A map of numeric values to scale
 * @return {Object} A copy of `rect` with values scaled up
 */
export function scaleUp (svg, rect) {
  if (arguments.length === 1) {
    rect = svg
  }

  const result = {}
  const viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport

  Object.keys(rect).forEach((key) => {
    result[key] = rect[key] * viewport.scale
  })

  return result
}

/**
 * Adjust scale from rendered scale to a normalized scale (100%).
 *
 * @param {Object} rect A map of numeric values to scale
 * @return {Object} A copy of `rect` with values scaled down
 */
export function scaleDown (rect: Record<string, number>) : Record<string, number> {
  // TODO for old style:  scaleDown(svg, rect)
  if (arguments.length === 2) {
    rect = arguments[1]
  }

  const result : Record<string, number> = {}
  const viewport = window.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  Object.keys(rect).forEach((key) => {
    result[key] = rect[key] / viewport.scale
  })

  return result
}

/**
 * Disable all text layers.
 */
export function disableTextlayer () {
  $('body').addClass('disable-text-layer')
}

/**
 * Enable all text layers.
 */
export function enableTextlayer () {
  $('body').removeClass('disable-text-layer')
}
