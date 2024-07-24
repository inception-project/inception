/**
 * Convert the `y` position from the local coords to exported json.
 */
export function convertToExportY (y) {
  const meta = getPageSize()

  y -= paddingTop

  const pageHeight = meta.height + paddingBetweenPages

  const pageNumber = Math.floor(y / pageHeight) + 1
  const yInPage = y - (pageNumber - 1) * pageHeight

  return { pageNumber, y: yInPage }
}

/**
 * The padding of page top.
 */
const paddingTop = 9

/**
 * The padding between pages.
 */
export const paddingBetweenPages = 9

/**
 * Get a page size of a single PDF page.
 */
export function getPageSize () : {width: number, height: number} {
  const pdfView = globalThis.PDFView

  const viewBox = pdfView.pdfViewer.getPageView(0).viewport.viewBox
  const size = { width: viewBox[2], height: viewBox[3] }
  return size
}
