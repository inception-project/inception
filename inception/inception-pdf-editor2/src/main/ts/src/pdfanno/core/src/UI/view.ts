/**
 * Deselect annotations when pages clicked.
 */
function handlePageClick (e) {
  globalThis.annotationContainer
    .getSelectedAnnotations()
    .forEach(a => a.deselect())

  const event = document.createEvent('CustomEvent')
  event.initCustomEvent('annotationDeselected', true, true, this)
  window.dispatchEvent(event)
}

/**
 * Enable view mode.
 */
export function enableViewMode () {
  document.querySelectorAll('.page').forEach(e => {
    e.removeEventListener('click', handlePageClick)
    e.addEventListener('click', handlePageClick)
  })
}
