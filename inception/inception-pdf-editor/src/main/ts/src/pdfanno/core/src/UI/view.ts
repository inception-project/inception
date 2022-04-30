/**
 * Deselect annotations when pages clicked.
 */
function handlePageClick (e) {
  window.annotationContainer
    .getSelectedAnnotations()
    .forEach(a => a.deselect())

  var event = document.createEvent('CustomEvent')
  event.initCustomEvent('annotationDeselected', true, true, this)
  window.dispatchEvent(event)
}

// TODO NO NEED `enableViewMode` event ?

/**
 * Enable view mode.
 */
export function enableViewMode () {
  // console.log('view:enableViewMode')

  document.querySelectorAll('.page').forEach(e => {
    e.removeEventListener('click', handlePageClick)
    e.addEventListener('click', handlePageClick)
  })
}
