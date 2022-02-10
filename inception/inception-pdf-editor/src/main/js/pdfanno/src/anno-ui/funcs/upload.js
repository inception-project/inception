/**
 * Functions - upload and analyze a PDF.
 */
export function upload ({
    contentFile,
    willStartCallback = function () {},
    progressCallback = function () {},
    successCallback = function () {},
    failedCallback = function () {}
} = {}) {

    // Convert PDF to base64 string.
    const contentBase64 = arrayBufferToBase64(contentFile.content)

    // API endpoint.
    const url = window.API_ROOT + 'internal/api/pdfs/' + contentFile.name

    // API params.
    let data = {
        pdf : contentBase64
    }

    // Callback before ajax call.
    willStartCallback()

    // Call the API.
    $.ajax({
        xhr : function () {
            var xhr = new window.XMLHttpRequest()
            // Upload progress
            xhr.upload.addEventListener('progress', function (evt) {
                if (evt.lengthComputable) {
                    var percentComplete = evt.loaded / evt.total
                    // Do something with upload progress
                    console.log('uploadProgress:', percentComplete)
                    let percent = Math.floor(percentComplete * 100)
                    progressCallback(percent)
                }
            }, false)

            return xhr
        },
        url      : url,
        method   : 'POST',
        dataType : 'json',
        data

    }).then(result => {
        if (result.status === 'failure') {
            failedCallback(result.err.stderr || result.err || result)
            return
        }

        setTimeout(() => {
            successCallback(result.text)
        }, 500) // wait for progress bar animation.
    })
}

function arrayBufferToBase64 (buffer) {
    var s = ''
    var bytes = new Uint8Array(buffer)
    var len = bytes.byteLength
    for (var i = 0; i < len; i++) {
        s += String.fromCharCode(bytes[i])
    }
    return window.btoa(s)
}
