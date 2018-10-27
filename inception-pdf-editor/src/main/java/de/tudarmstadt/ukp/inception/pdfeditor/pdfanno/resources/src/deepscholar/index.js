/**
 * PDFAnno for DeepScholar.
 *
 * Refs:
 *  https://github.com/paperai/paperanno-ja/blob/master/with-deepscholar.md
 */
import { parseUrlQuery } from '../shared/util'
import * as textLayer from '../page/textLayer'
import { showLoader } from '../page/util/display'
import * as annoUI from 'anno-ui'

// arxiv1103.1918

// Get data from URL query.
const params = parseUrlQuery()
const apiRoot = params['api_root']
const paperId = params['paper_id']
const userToken = params['user_token']
const userId = params['user_id']
const moveTo = params['move']

/**
 * Check whether PDFAnno should behave for DeepScholar.
 */
export function isTarget () {
  return apiRoot && paperId
}

/**
 * Init for displaying for DeepScholar.
 */
export async function initialize () {

  // UI for DeepScholar.
  $(document.body).addClass('deepscholar')

  try {

    const data = await fetchResources()

    displayPDF(data.pdf, data.pdfName)

    addTextLayer(data.pdftxt)

    displayDocumentName()

    window.annoPage.annoFiles = data.annotations.map(a => {
      return { name : a.userId, content : a.anno }
    })

    showPrimaryAnnotation(data.annotations)

    showReferenceAnnotation(data.annotations)

    setupUploadButton()

  } catch (e) {
    alert('Error!!')
    console.log('deepscholer view error:', e)

  } finally {
    showLoader(false)
  }

}

/**
 * Fetch the resources from API - PDF, pdftxt, annotations.
 * @returns {Promise<*>}
 */
async function fetchResources () {
  const params = parseUrlQuery()
  let url = window.API_ROOT + `internal/api/deepscholar/${paperId}`
  const queries = Object.keys(params).map(key => {
    return `${key}=${params[key]}`
  })
  url += '?' + queries.join('&')

  const response = await fetch(url)
  if (response.status !== 200) {
    return annoUI.ui.alertDialog.show({ message : 'API Error.' })
  }
  const data = await response.json()

  addLogs(data.logs)

  return data
}

/**
 * Upload annotations
 * @param anno
 * @returns {Promise<Error>}
 */
async function uploadAnnotation (anno) {

  const url = window.API_ROOT + `internal/api/deepscholar/${paperId}/annotations`
  const response = await fetch(url, {
    method : 'PUT',
    body   : JSON.stringify({
      api_root : apiRoot,
      token    : userToken,
      anno
    }),
    headers : new Headers({ 'Content-type' : 'application/json' })
  })
  console.log('response:', response)
  const body = await response.json()
  console.log(response.status, body)
  if (response.status !== 200) {
    return body.message
  }
  return null
}

/**
 * Display a PDF.
 * @param pdfBase64 - PDF from DeepScholar.
 * @param pdfName - the PDF name.
 */
function displayPDF (pdfBase64, pdfName) {

  const pdf = Uint8Array.from(atob(pdfBase64), c => c.charCodeAt(0))

  // Display PDF.
  window.annoPage.displayViewer({
    name    : pdfName,
    content : pdf
  })
}

/**
 * Add text layers.
 * @param pdftxt - pdftxt from DeepScholar.
 */
function addTextLayer (pdftxt) {
  textLayer.setup(pdftxt)
  window.annoPage.pdftxt = pdftxt
}

function displayDocumentName () {
  $('#dropdownPdf').addClass('no-action')
  $('#dropdownPdf .js-text').text(paperId)
  $('#dropdownPdf .caret').remove()
}

/**
 * Display and setup the primary annotations.
 * @param annotations - annotations from DeepScholar.
 */
function showPrimaryAnnotation (annotations) {

  setTimeout(() => {
    annotations = annotations.filter(annotation => annotation.userId === userId)
    if (annotations.length > 0) {
      const anno = annotations[0].anno
      console.log('anno:', anno)
      const results = window.addAll(anno)

      if (moveTo && results[moveTo - 1]) {
        setTimeout(() => {
          const annotation = results[moveTo - 1].annotation
          window.annoPage.scrollToAnnotation(annotation.uuid)
        }, 500)
      }
    }
  }, 1000)  // wait until viewer rendered.

  $('#dropdownAnnoPrimary').addClass('no-action')
  $('#dropdownAnnoPrimary .js-text').text(userId)
  $('#dropdownAnnoPrimary .caret').remove()

}

/**
 * Setup the reference annotations.
 * @param annotations - annotations from DeepScholar.
 */
function showReferenceAnnotation (annotations) {
  // TODO あとでデータができたら実装する.
  console.log('annotations:', annotations)

  // userIdを元に、自分以外のアノテーションを取得.
  // ReferenceAnnoのセレクトボックスに表示.

  annotations = annotations.filter(annotation => annotation.userId !== userId)
  setAnnoDropdownList(annotations)
}

/**
 * Setup the upload button.
 */
function setupUploadButton () {

  $('#deepschoarUploadButton').off().on('click', async () => {

    // TODO 実装する.
    // userIdがない状態でボタンを押したら、エラー表示.
    // Logタブにも表示.

    // Get current annotations.
    const anno = await window.annoPage.exportData({ exportType : 'json' })

    // Upload.
    const err = await uploadAnnotation(anno)
    if (err) {
      alert(err)  // TODO Dialog UI.
    } else {
      alert('Success.')  // TODO Dialog UI.
    }

  })

}

/**
 * Reset and setup the primary/reference annotation dropdown.
 */
function setAnnoDropdownList (annotations) {

  // Reset the UI of primary/reference anno dropdowns.
  $('#dropdownAnnoReference ul').html('')
  $('#dropdownAnnoReference .js-text').text('Reference Annos')

  // Setup anno / reference dropdown.
  annotations.forEach(anno => {

    let snipet = `
            <li>
                <a href="#">
                    <i class="fa fa-check no-visible" aria-hidden="true"></i>
                    <span class="js-annoname">${anno.userId}</span>
                </a>
            </li>
        `
    $('#dropdownAnnoReference ul').append(snipet)
  })

}

function addLogs (logs) {
  logs.forEach(log => {
    let text = $('#uploadResultDummy').val()
    if (text) {
      text += '\n'
    }
    text += log
    $('#uploadResultDummy').val(text)
  })
}
