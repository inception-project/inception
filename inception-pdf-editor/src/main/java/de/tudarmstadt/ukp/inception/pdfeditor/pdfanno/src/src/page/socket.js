/**
 Websocket client for PDFAnno.
 */

let socket

export function setup () {

  const script = document.createElement('script')
  script.type = 'text/javascript'
  script.src = window.API_ROOT + 'socket.io/socket.io.js'
  script.onload = socketReady
  document.head.appendChild(script)

  function socketReady () {

    socket = window.io(window.API_DOMAIN + '/ws', {
      path : window.API_PATH + 'socket.io'
    })
    console.log('socket:', socket)

    socket.on('connect', function () {
      console.log('connected front!!')
    })

    socket.on('annotationUpdated', function (message) {
      let text = $('#uploadResultDummy').val()
      if (text) {
        text += '\n'
      }
      $('#uploadResultDummy').val(text + message)
      console.log('annotationUpdated:', message)
    })
  }
}

export function sendAnnotationUpdated (data) {
  // send('annotation', data)
  console.log(data.updated)
}

function send (topic, data) {

  if (socket) {
    socket.emit(topic, data)

  } else {
    console.log('socket haven\'t be initialized yet.')
  }
}
