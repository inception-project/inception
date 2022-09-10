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
