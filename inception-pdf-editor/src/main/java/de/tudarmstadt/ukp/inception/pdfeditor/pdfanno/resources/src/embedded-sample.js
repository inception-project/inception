require('file-loader?name=embedded-sample.html!./embedded-sample.html')
require('!style-loader!css-loader!./embedded-sample.css')

// sample.
// ?pdf=https://yoheim.net/tmp/bitcoin.pdf&anno=https://yoheim.net/tmp/bitcoin.anno

// TODO 後でコア機能は別JSに移してもいいかも.
import URI from 'urijs' // TODO これが意外と重たいようだ... setTimeoutで時間かかりすぎの警告が出ている.
import PDFAnnoPage from './page/pdf/PDFAnnoPage'
import * as publicApi from './page/public'
import { unlistenWindowLeaveEvent } from './page/util/window'

// XXX
process.env.SERVER_PATH = '0.4.1'

/**
 * API root point.
 */
if (process.env.NODE_ENV === 'production') {
  window.API_DOMAIN = 'https://pdfanno.hshindo.com'
  window.API_PATH = '/' + process.env.SERVER_PATH + '/'
  window.API_ROOT = window.API_DOMAIN + window.API_PATH
} else {
  window.API_DOMAIN = 'http://localhost:3000'
  window.API_PATH = '/'
  window.API_ROOT = window.API_DOMAIN + window.API_PATH
}

// ServiceWorker.
(async () => {
  if ('serviceWorker' in navigator) {
    try {
      const registration = navigator.serviceWorker.register('./sw.js')
      console.log('ServiceWorker registration successed. with scope: ' + registration.scope)
    } catch (err) {
      console.log('ServiceWorker registration failed. reason: ' + err)
    }
  }
})()

window.annoPage = new PDFAnnoPage()

function getPDFUrlFromQuery () {
  return URI(document.URL).query(true).pdf
}

function getAnnoUrlFromQuery () {
  return URI(document.URL).query(true).anno
}

function getPDFName (url) {
  const a = url.split('/')
  return a[a.length - 1]
}

function initViewer () {
  return new Promise((resolve, reject) => {
    window.addEventListener('iframeReady', () => {
      // setTimeout(resolve, 500, true)
      // setTimeout(() => {
      //     window.annoPage.displayViewer({
      //         name    : getPDFName(pdfUrl),
      //         content : pdf
      //     })
      // }, 500)
      resolve(true)
    })
    // Create a viewer without a pdf.
    window.annoPage.initializeViewer(null)
    window.annoPage.startViewerApplication()
  })
}

function waitUntilRendered () {
  return new Promise((resolve, reject) => {
    const listenPageRendered = () => {
      window.removeEventListener('pagerendered', listenPageRendered)
      resolve(true)
    }
    window.addEventListener('pagerendered', listenPageRendered)
  })
}

window.addEventListener('DOMContentLoaded', async () => {

  const pdfUrl = getPDFUrlFromQuery()
  if (!pdfUrl) {
    return
  }

  // Create a viewer, and load/display a PDF.
  let [ ok1, { pdf } ] = await Promise.all([
    initViewer(),
    window.annoPage.loadPDFFromServer(pdfUrl)
  ])
  console.log('ok:', ok1)
  window.annoPage.displayViewer({
    name    : getPDFName(pdfUrl),
    content : pdf
  })

  const annoUrl = getAnnoUrlFromQuery()
  if (!annoUrl) {
    return
  }

  // Show annotations.
  let [ ok2, anno ] = await Promise.all([
    waitUntilRendered(),
    window.annoPage.loadAnnoFileFromServer(annoUrl)
  ])
  console.log('ok:', ok2)
  publicApi.addAllAnnotations(publicApi.readTOML(anno))

  // const listenPageRendered = async () => {
  //     // Load and display annotations.
  //     let anno = await window.annoPage.loadAnnoFileFromServer(annoUrl)
  //     console.log('anno:', anno)
  //     publicApi.addAllAnnotations(publicApi.readTOML(anno))
  //     window.removeEventListener('pagerendered', listenPageRendered)
  // }
  // window.addEventListener('pagerendered', listenPageRendered)

  // temporary.
  setInterval(unlistenWindowLeaveEvent, 500)
})
