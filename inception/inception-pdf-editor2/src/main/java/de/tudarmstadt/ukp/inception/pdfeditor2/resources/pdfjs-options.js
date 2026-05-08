/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * PDF.js Viewer Options Configuration
 *
 * This script sets up PDF.js viewer options at the correct time during
 * initialization. The approach uses the "webviewerloaded" event which is:
 * 1. Dispatched AFTER window.PDFViewerApplicationOptions is set by viewer.mjs
 * 2. Dispatched BEFORE PDFViewerApplication.run(config) is called
 *
 * This is the only reliable hook point to configure options before the viewer
 * reads them and starts initializing.
 */
(function (iframeWindow) {
  'use strict'
  if (typeof iframeWindow === 'undefined') return

  function configurePdfJsOptions (sourceWindow) {
    // Use the source window from the event, which is the iframe's window where
    // PDFViewerApplicationOptions is defined. This is critical because when the
    // event is dispatched to parent.document, 'window' in the handler refers to
    // the parent window, not the iframe window.
    var targetWindow = sourceWindow || iframeWindow
    var opts = targetWindow.PDFViewerApplicationOptions
    if (!opts || typeof opts.set !== 'function') {
      console.warn('[pdfjs-options] PDFViewerApplicationOptions not available on target window')
      return
    }

    console.log('[pdfjs-options] Configuring PDFViewerApplicationOptions via webviewerloaded event')

    // Core viewer options - disable PDF.js built-in annotation rendering
    // so INCEpTION can render its own annotations
    opts.set('annotationMode', 0) // 0 = DISABLE (completely disable annotation rendering)
    opts.set('annotationEditorMode', -1) // -1 = Disable editor mode

    // Disable preferences to prevent localStorage from overriding our settings
    opts.set('disablePreferences', true)

    // Clear any existing PDF.js preferences from localStorage
    // Use the target window's localStorage for consistency
    var storage = targetWindow.localStorage
    if (storage) {
      try {
        Object.keys(storage).forEach(function (k) {
          if (k.startsWith('pdfjs.')) storage.removeItem(k)
        })
      } catch (e) {
        /* Ignore localStorage errors */
      }
    }

    // Resource paths - these are relative to the viewer.html location
    opts.set('workerSrc', 'pdf.worker.min.mjs')
    opts.set('sandboxBundleSrc', 'pdf.sandbox.min.mjs')
    opts.set('wasmUrl', 'wasm/')
    opts.set('cMapUrl', 'cmaps/')
    opts.set('imageResourcesPath', 'image_decoders/')
    opts.set('standardFontDataUrl', 'standard_fonts/')
    opts.set('cMapPacked', true)

    // Security and scripting
    opts.set('isEvalSupported', false)
    opts.set('enableScripting', false)
    opts.set('externalLinkEnabled', false)
    opts.set('externalLinkTarget', 0)

    // Default URL - don't load any document automatically
    opts.set('defaultUrl', null)

    // View settings
    opts.set('viewOnLoad', 1)
    opts.set('sidebarViewOnLoad', 0)

    console.log('[pdfjs-options] Configuration complete')
  }

  // The webviewerloaded event is dispatched by viewer.mjs AFTER it sets
  // window.PDFViewerApplicationOptions but BEFORE it calls
  // PDFViewerApplication.run(config). This is the perfect hook point.
  //
  // The event includes detail.source which is the iframe's window - we must use
  // this to access PDFViewerApplicationOptions, because when dispatched to
  // parent.document, the 'window' in the handler context is the parent window.
  function addListeners () {
    var handler = function (event) {
      // Get the source window from the event detail - this is the iframe window
      // where viewer.mjs is running and where PDFViewerApplicationOptions is set
      var sourceWindow = event && event.detail && event.detail.source
      configurePdfJsOptions(sourceWindow)
    }
    // Listen on both iframe document and parent document to catch the event
    // regardless of which one it's dispatched to
    iframeWindow.document.addEventListener('webviewerloaded', handler, { once: true })
    try {
      if (iframeWindow.parent && iframeWindow.parent.document !== iframeWindow.document) {
        iframeWindow.parent.document.addEventListener('webviewerloaded', handler, { once: true })
      }
    } catch (e) {
      /* Cross-origin iframe - ignore */
    }
  }

  addListeners()
  console.log('[pdfjs-options] Waiting for webviewerloaded event')
})(window)
