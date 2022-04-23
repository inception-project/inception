import $ from '../../../shared/jquery'
import 'jquery-ui';

/**
 * UI - Alert dialog.
 */
import './index.css'

export function create ({ type = 'alert', message = '' }) {
    const id = 'modal-' + (new Date().getTime())

    const styleClass = (type === 'alert' ? 'alertdialog-danger' : '')

    const snipet = `
        <div id="${id}" class="alertdialog modal fade ${styleClass}" role="dialog">
          <div class="modal-dialog">
            <div class="modal-content">
              <div class="modal-header">
                <h4 class="modal-title">Error</h4>
              </div>
              <div class="modal-body">
                <p>${message}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
              </div>
            </div>
          </div>
        </div>
    `
    $(document.body).append(snipet)

    return $('#' + id)
}

export function show () {
    const $modal = create(...arguments)
    $modal.modal('show')
    return $modal
}
