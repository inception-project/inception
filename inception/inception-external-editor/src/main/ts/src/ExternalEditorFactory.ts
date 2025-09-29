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
import { AnnotationEditor, AnnotationEditorFactory, AnnotationEditorProperties, DiamClientFactory } from '@inception-project/inception-js-api'

const PROP_EDITOR = '__editor__'

export class ExternalEditorFactory implements AnnotationEditorFactory {
  async getOrInitialize (element: Node, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    if (element[PROP_EDITOR] != null) {
      return element[PROP_EDITOR]
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

      element[PROP_EDITOR] = await this.loadIFrameContent(iframe)
        .then(win => this.loadEditorResources(win, props))
        .then(win => this.installKeyEventForwarding(win, element.ownerDocument.defaultView))
        .then(win => {
          if (this.isDocumentJavascriptCapable(win.document)) {
            // On HTML documents provide the body element as target to the editor
            return this.initEditor(win, win.document.getElementsByTagName('body')[0], diam, props)
          }

          // On XML documents, provide the document root as target to the editor
          return this.initEditor(window, win.document, diam, props)
        })

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
      iframe.src = 'about:blank'
      const eventHandler = () => {
        iframe.removeEventListener('load', eventHandler)
        const content = iframe.contentDocument || iframe.contentWindow?.document
        console.debug(`IFrame has loaded: ${content?.location}`)
        resolve(iframe.contentWindow)
      }
      console.debug('Waiting for IFrame content to load...')
      iframe.addEventListener('load', eventHandler)
      iframe.src = iframeUrl
    })
  }

  installKeyEventForwarding (source: Window, target: Window): Promise<Window> {
    return new Promise(resolve => {
      console.debug('Installing key event forwarding...')

      if (source !== target) {
        source.addEventListener('keydown', event => {
          console.debug(`Forwarding keydown event: ${event.key}`)
          const delegate = target.document.body || target.document
          delegate.dispatchEvent(new KeyboardEvent('keydown', event))
        })

        source.addEventListener('keyup', event => {
          console.debug(`Forwarding keyup event: ${event.key}`)
          const delegate = target.document.body || target.document
          delegate.dispatchEvent(new KeyboardEvent('keyup', event))
        })

        source.addEventListener('keypress', event => {
          console.debug(`Forwarding keypress event: ${event.key}`)
          const delegate = target.document.body || target.document
          delegate.dispatchEvent(new KeyboardEvent('kekeypressyup', event))
        })
      }

      resolve(source)
    })
  }

  loadEditorResources (win: Window, props: AnnotationEditorProperties): Promise<Window> {
    return new Promise(resolve => {
      console.debug('Preparing to load editor resources...')

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
      const allPromises: Promise<void>[] = []
      if (this.isDocumentStylesheetCapable(win.document) && props.stylesheetSources) {
        allPromises.push(...props.stylesheetSources.map(src => this.loadStylesheet(target, src)))
      }

      if (props.scriptSources) {
        allPromises.push(...props.scriptSources.map(src => this.loadScript(target, src)))
      }

      Promise.all(allPromises).then(() => {
        console.info(`${allPromises.length} editor resources loaded`)
        resolve(win)
      })
    })
  }

  initEditor (contextWindow: Window, targetElement: HTMLElement | Document, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    return new Promise(resolve => {
      console.debug('Preparing to initialize editor...')

      const editorFactory = (contextWindow as any).eval(props.editorFactory) as AnnotationEditorFactory
      editorFactory.getOrInitialize(targetElement, diam, props).then(editor => {
        console.info('Editor initialized')
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
