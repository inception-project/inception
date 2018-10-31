/**
 * Disable the action of pageback, if `DEL` or `BackSpace` button pressed.
 */
function disablePagebackAction (e) {
  // Allow any keyboard events for <input/>.
  if (e.target.tagName.toLowerCase() === 'input') {
    return
  }

  // Delete or BackSpace.
  if (e.keyCode === 46 || e.keyCode === 8) {
    e.preventDefault()

    if (e.type === 'keyup') {
      deleteSelectedAnnotations()
    }

    return false
  }
}

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

/**
 * Delete selected annotations.
 */
function deleteSelectedAnnotations () {
  window.globalEvent.emit('deleteSelectedAnnotation')
}

// TODO NO NEED `enableViewMode` event ?

/**
 * Enable view mode.
 */
export function enableViewMode () {
  console.log('view:enableViewMode')

  document.removeEventListener('keyup', disablePagebackAction)
  document.removeEventListener('keydown', disablePagebackAction)
  document.addEventListener('keyup', disablePagebackAction)
  document.addEventListener('keydown', disablePagebackAction)

  $(document)
    .off('click', handlePageClick)
    .on('click', '.page', handlePageClick)
}
