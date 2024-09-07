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
import type { AnnotationEditorFactory, AnnotationEditorProperties, DiamClientFactory } from '@inception-project/inception-js-api'
import { RecogitoEditor } from './RecogitoEditor'

const PROP_EDITOR = '__editor__'

export class RecogitoEditorFactory implements AnnotationEditorFactory {
  public async getOrInitialize (element: Node, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<RecogitoEditor> {
    if (element[PROP_EDITOR] != null) {
      return element[PROP_EDITOR]
    }

    const ajax = diam.createAjaxClient(props.diamAjaxCallbackUrl)

    let targetElement: Element | null = null

    if ((element as any).querySelector) {
      targetElement = (element as any).querySelector('[data-capture-root]')
    }

    if (!targetElement && element instanceof HTMLDocument) {
      targetElement = element.body
    }

    if (!targetElement) {
      targetElement = element as Element
    }

    // If the target element is the body of a HTML document and the body has only one child, then
    // we use the child as the target element. This is to support the case where the body is
    // used as a container for the additional editor functionality.
    const body = element.ownerDocument?.body
    if (body) {
      // Add a (scrolling) wrapper in the body
      const wrapper = element.ownerDocument.createElement('div')
      wrapper.classList.add('i7n-wrapper')
      wrapper.style.overflow = 'auto'

      if (body === targetElement) {
        Array.from(targetElement.childNodes).forEach((child) => wrapper.appendChild(child))
        targetElement = wrapper
      }
      else  {
        Array.from(body.childNodes).forEach((child) => wrapper.appendChild(child))
      }

      body.appendChild(wrapper)

      // Configure a "full screen" flex layout on the body
      body.style.display = 'flex'
      body.style.flexDirection = 'column'
      body.style.height = '100vh'
      body.style.width = '100vw'
      body.style.overflow = 'hidden'
    }

    element[PROP_EDITOR] = new RecogitoEditor(targetElement, ajax, props.userPreferencesKey)
    return element[PROP_EDITOR]
  }

  public destroy (element: Node) {
    if (element[PROP_EDITOR] != null) {
      element[PROP_EDITOR].destroy()
      console.log('Destroyed RecogitoJS')
    }
  }
}
