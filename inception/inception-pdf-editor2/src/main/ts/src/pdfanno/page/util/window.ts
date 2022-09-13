/**
 * Utility for window.
 */

/**
 Adjust the height of viewer according to window height.
 */
export function adjustViewerSize () {
  window.removeEventListener('resize', resizeHandler)
  window.addEventListener('resize', resizeHandler)
  resizeHandler()
}

/**
 * Resize the height of elements adjusting to the window.
 */
export function resizeHandler () {
  // PDFViewer.
  const viewer = document.getElementById('viewerInner')
  if (viewer === null) {
    return
  }

  const height = window.innerHeight - viewer.getBoundingClientRect().top + window.scrollY
  viewer.style.height = `${height}px`

  document.querySelectorAll('.loadingInProgress').forEach(
    e => { (e as HTMLElement).style.height = '100%' })
}
