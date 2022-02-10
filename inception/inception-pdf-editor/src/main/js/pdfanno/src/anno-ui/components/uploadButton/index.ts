/**
 * UI parts - Upload Button.
 */
import * as alertDialog from '../../uis/alertDialog'
import { upload } from '../../funcs/upload'

export function setup ({
    getCurrentDisplayContentFile,
    uploadFinishCallback = function () {}
}) {
    $('.js-btn-upload').off('click').on('click', () => {
        const contentFile = getCurrentDisplayContentFile()
        uploadPDF({
            contentFile,
            successCallback : uploadFinishCallback
        })
        return false
    })
}

export function uploadPDF ({
    contentFile,
    successCallback = function () {}
}) {

    if (!contentFile) {
        return alertDialog.show({ message : 'Display a content before upload.' })
    }

    // Progress bar.
    const $progressBar = $('.js-upload-progress')

    // Upload and analyze the PDF.
    upload({
        contentFile,
        willStartCallback : () => {
            // Reset the result text.
            setResult('Waiting for response...')
            // Show the progress bar.
            $progressBar.removeClass('hidden').find('.progress-bar').css('width', '0%').attr('aria-valuenow', 0).text('0%')
        },
        progressCallback : percent => {
            $progressBar.find('.progress-bar').css('width', percent + '%').attr('aria-valuenow', percent).text(percent + '%')
            if (percent === 100) {
                setTimeout(() => {
                    $progressBar.addClass('hidden')
                }, 2000)
            }
        },
        successCallback : resultText => {
            setResult(resultText)
            successCallback(resultText)
        },
        failedCallback : err => {
            const message = 'Failed to upload and analyze your PDF.<br>Reason: ' + err
            alertDialog.show({ message })
            setResult(err)
        }
    })
}

/**
 * Set the analyzing result.
 */
export function setResult (text) {
    $('#uploadResult').val(text)
}
