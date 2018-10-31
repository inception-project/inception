/**
 * Convert the `y` position from the local coords to exported json.
 */
export function convertToExportY (y) {

  let meta = getPageSize()

  y -= paddingTop

  let pageHeight = meta.height + paddingBetweenPages

  let pageNumber = Math.floor(y / pageHeight) + 1
  let yInPage = y - (pageNumber - 1) * pageHeight

  return { pageNumber, y : yInPage }
}

/**
 * Convert the `y` position from exported json to local coords.
 */
export function convertFromExportY (pageNumber, yInPage) {

  let meta = getPageSize()

  let y = yInPage + paddingTop

  let pagePadding = paddingBetweenPages

  y += (pageNumber - 1) * (meta.height + pagePadding)

  return y
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
export function getPageSize () {

  let pdfView = window.PDFView || window.iframeWindow.PDFView

  let viewBox = pdfView.pdfViewer.getPageView(0).viewport.viewBox
  let size = { width : viewBox[2], height : viewBox[3] }
  return size
}

/**
 * Get the next z-index.
 */
export function nextZIndex () {

  let w = (window.iframeWindow ? window.iframeWindow : window)

  if (!w.nextZIndex) {
    w.nextZIndex = 10
  }

  return w.nextZIndex++
}
