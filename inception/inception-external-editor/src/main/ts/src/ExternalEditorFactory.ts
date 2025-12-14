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
import { type AnnotationEditor, type AnnotationEditorFactory, type AnnotationEditorProperties, type DiamClientFactory } from '@inception-project/inception-js-api'

const PROP_EDITOR = '__editor__'
const PROP_INITIALIZING = '__initializing__'

export class ExternalEditorFactory implements AnnotationEditorFactory {
  async getOrInitialize (element: Node, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    // If already initialized, return the existing editor
    if (element[PROP_EDITOR] != null) {
      console.debug('[getOrInitialize] Editor already exists, returning existing instance')
      return element[PROP_EDITOR]
    }

    // If initialization is in progress, wait for it
    if (element[PROP_INITIALIZING] != null) {
      console.debug('[getOrInitialize] Initialization already in progress, waiting for existing promise')
      return await element[PROP_INITIALIZING]
    }

    if (element instanceof HTMLIFrameElement) {
      const iframe = element as HTMLIFrameElement
      let loadingIndicator : HTMLDivElement | undefined

      // Hiding editor because loading the editor resources and in particular CSS files for large
      // documents can take a while. This might cause the browser to render the document first
      // without CSS and then re-render with CSS - which causes an undesired "flickering"
      if (!props.loadingIndicatorDisabled) {
        iframe.style.display = 'none'
        loadingIndicator = document.createElement('div')
        loadingIndicator.classList.add('flex-content')
        loadingIndicator.style.display = 'flex'
        loadingIndicator.style.justifyContent = 'center'
        loadingIndicator.style.alignItems = 'center'

        const spinner = document.createElement('div')
        spinner.classList.add('spinner-border')
        spinner.classList.add('text-muted')
        spinner.setAttribute('role', 'status')

        const spinnerText = document.createElement('span')
        spinnerText.classList.add('visually-hidden')
        spinnerText.innerText = 'Loading...'
        spinner.appendChild(spinnerText)
        loadingIndicator.appendChild(spinner)

        iframe.before(loadingIndicator)
      }

      console.debug('[getOrInitialize] Starting promise chain for iframe')
      // Store the initialization promise to prevent duplicate initialization
      element[PROP_INITIALIZING] = this.loadIFrameContent(iframe)
        .then(win => {
          console.debug('[getOrInitialize] loadIFrameContent resolved, calling loadEditorResources')
          return this.loadEditorResources(win, props)
        })
        .then(win => {
          console.debug('[getOrInitialize] loadEditorResources resolved, calling installKeyEventForwarding')
          return this.installKeyEventForwarding(win, element.ownerDocument.defaultView)
        })
        .then(win => {
          console.debug('[getOrInitialize] installKeyEventForwarding resolved, calling initEditor')
          if (this.isDocumentJavascriptCapable(win.document)) {
            // On HTML documents provide the body element as target to the editor
            return this.initEditor(win, win.document.getElementsByTagName('body')[0], diam, props)
          }

          // On XML documents, provide the document root as target to the editor
          return this.initEditor(window, win.document, diam, props)
        })
      
      element[PROP_EDITOR] = await element[PROP_INITIALIZING]
      delete element[PROP_INITIALIZING]
      console.debug('[getOrInitialize] Promise chain completed')

      // Restoring visibility
      if (!props.loadingIndicatorDisabled) {
        loadingIndicator?.remove()
        iframe.style.display = ''
      }

      return element[PROP_EDITOR]
    }

    element[PROP_EDITOR] = await this.loadEditorResources(window, props)
      .then(win => {
        return this.initEditor(win, element as HTMLElement, diam, props)
      })

    return element[PROP_EDITOR]
  }

  loadIFrameContent (iframe: HTMLIFrameElement): Promise<Window> {
    return new Promise(resolve => {
      const iframeUrl = iframe.src
      console.debug(`[loadIFrameContent] Starting - iframe.src: ${iframeUrl}`)
      console.debug(`[loadIFrameContent] Setting iframe.src to about:blank`)
      iframe.src = 'about:blank'
      const eventHandler = () => {
        iframe.removeEventListener('load', eventHandler)
        const content = iframe.contentDocument || iframe.contentWindow?.document
        console.debug(`[loadIFrameContent] Load event fired - document location: ${content?.location}, readyState: ${content?.readyState}`)
        console.debug(`[loadIFrameContent] Resolving promise with iframe window`)
        resolve(iframe.contentWindow)
      }
      console.debug('[loadIFrameContent] Adding load event listener')
      iframe.addEventListener('load', eventHandler)
      console.debug(`[loadIFrameContent] Setting iframe.src to: ${iframeUrl}`)
      iframe.src = iframeUrl
      console.debug('[loadIFrameContent] iframe.src set, waiting for load event...')
    })
  }

  installKeyEventForwarding (source: Window, target: Window): Promise<Window> {
    return new Promise(resolve => {
      console.debug('Installing key event forwarding...')

      if (source !== target) {
        const shouldForward = (event: Event): boolean => {
          const targetElement = event.target as HTMLElement | null
          if (!targetElement) return true

          const tagName = targetElement.tagName?.toLowerCase()
          const isEditable =
            tagName === 'input' ||
            tagName === 'textarea' ||
            targetElement.isContentEditable

          return !isEditable
        }

        const forwardEvent = (type: string, event: KeyboardEvent) => {
          if (!shouldForward(event)) return

          console.debug(`Forwarding ${type} event: ${event.key}`)
          const delegate = target.document.body || target.document
          delegate.dispatchEvent(new KeyboardEvent(type, event))
        }

        source.addEventListener('keydown', e => forwardEvent('keydown', e))
        source.addEventListener('keyup', e => forwardEvent('keyup', e))
        source.addEventListener('keypress', e => forwardEvent('keypress', e))
      }

      resolve(source)
    })
  }

  loadEditorResources (win: Window, props: AnnotationEditorProperties): Promise<Window> {
    return new Promise(resolve => {
      console.debug(`[loadEditorResources] Starting - document location: ${win.document.location}, readyState: ${win.document.readyState}`)

      // Make sure body is accessible via body property - seems the browser does not always ensure
      // this...
      if (win.document instanceof HTMLDocument || document.documentElement.constructor.name) {
        if (win.document.head == null) {
          const head = win.document.createElement('head')
          win.document.documentElement.insertBefore(head, win.document.body)
        }
  
        if (!document.body) {
          win.document.body = win.document.getElementsByTagName('body')[0]
        }
      }

      const target = this.isDocumentJavascriptCapable(win.document) ? win.document : document
      const targetDesc = target === win.document ? 'iframe document' : 'parent document'
      console.debug(`[loadEditorResources] Target for resources: ${targetDesc}`)
      
      const allPromises: Promise<void>[] = []
      if (this.isDocumentStylesheetCapable(win.document) && props.stylesheetSources) {
        allPromises.push(...props.stylesheetSources.map(src => this.loadStylesheet(target, src)))
      }

      if (props.scriptSources) {
        console.debug(`[loadEditorResources] Loading ${props.scriptSources.length} script(s)`)
        allPromises.push(...props.scriptSources.map(src => this.loadScript(target, src)))
      }

      Promise.all(allPromises).then(() => {
        console.info(`[loadEditorResources] ${allPromises.length} editor resources loaded - resolving`)
        resolve(win)
      })
    })
  }

  initEditor (contextWindow: Window, targetElement: HTMLElement | Document, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    return new Promise(resolve => {
      console.debug(`[initEditor] Starting - document location: ${contextWindow.document.location}, readyState: ${contextWindow.document.readyState}`)

      const editorFactory = (contextWindow as any).eval(props.editorFactory) as AnnotationEditorFactory
      console.debug('[initEditor] Calling editorFactory.getOrInitialize...')
      editorFactory.getOrInitialize(targetElement, diam, props).then(editor => {
        console.info('[initEditor] Editor initialized - resolving')
        resolve(editor)
      })
    })
  }

  isDocumentJavascriptCapable (document: Document): boolean {
    return document instanceof HTMLDocument || document.documentElement.constructor.name === 'HTMLHtmlElement'
  }

  isDocumentStylesheetCapable (document: Document): boolean {
    // We could theoretically also inject an XML-stylesheet processing instruction into the
    // IFrame, but probably we could not wait for the stylesheet to load as we do in HTML
    return document instanceof HTMLDocument || document.documentElement.constructor.name === 'HTMLHtmlElement'
  }

  loadStylesheet (document: Document, styleSheetSource: string): Promise<void> {
    return new Promise(resolve => {
      console.debug(`Preparing to load stylesheet: ${styleSheetSource} ...`)

      const css = document.createElement('link')
      css.rel = 'stylesheet'
      css.type = 'text/css'
      css.href = styleSheetSource
      css.onload = () => {
        console.info(`Loaded stylesheet: ${styleSheetSource}`)
        css.onload = null
        resolve()
      }

      const headElements = document.getElementsByTagName('head')
      if (headElements.length > 0) {
        headElements[0].appendChild(css)
      }
      else {
        console.warn(`Unable to register stylesheet: ${styleSheetSource} - no head element found`)
        resolve()
      }
    })
  }

  loadScript (document: Document, scriptSource: string): Promise<void> {
    return new Promise(resolve => {
      console.debug(`Preparing to load script: ${scriptSource} ...`)

      const script = document.createElement('script')
      script.src = scriptSource
      script.onload = () => {
        console.info(`Loaded script: ${scriptSource}`)
        script.onload = null
        resolve()
      }

      const headElements = document.getElementsByTagName('head')
      if (headElements.length > 0) {
        document.getElementsByTagName('head')[0].appendChild(script)
      }
      else {
        console.warn(`Unable to register script: ${scriptSource} - no head element found`)
        resolve()
      }
    })
  }

  destroy (element: Node): void {
    if (element[PROP_EDITOR] != null) {
      element[PROP_EDITOR].destroy()
      console.info('Destroyed editor')
    }
  }
}
