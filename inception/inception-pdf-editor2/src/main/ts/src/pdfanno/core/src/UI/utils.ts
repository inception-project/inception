import { annoLayer2Id } from '../../../pdfanno.js'

/**
 * Adjust scale from rendered scale to a normalized scale (100%).
 *
 * @param point A coordinate to scale
 * @return A of the coordinate with values scaled down
 */
export function scaleDown (point: { x: number, y: number }) : { x: number, y: number } {
  const viewport = globalThis.PDFViewerApplication.pdfViewer.getPageView(0).viewport
  return {
    x: point.x / viewport.scale,
    y: point.y / viewport.scale
  }
}

export function getClientXY (e: MouseEvent): { x: number, y: number } {
  const rect = document.getElementById(annoLayer2Id).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  return { x, y }
}

  /**
   * Transform the rotation and scale of a node using SVG's native transform attribute.
   *
   * @param node The node to be transformed
   * @param viewport The page's viewport data
   * @return {Node}
   */
  export function transform (base: HTMLElement, node: HTMLElement, viewport: any): HTMLElement {
    node.style.transform = `scale(${viewport.scale})`
    return node
  }
