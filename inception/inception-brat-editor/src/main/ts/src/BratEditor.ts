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
import type { AnnotationEditor, AnnotationEditorProperties, DiamAjax, Offsets } from '@inception-project/inception-js-api'
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'
import { Ajax } from './ajax/Ajax'
import { AnnotatorUI } from './annotator_ui/AnnotatorUI'
import { Dispatcher } from './dispatcher/Dispatcher'
import type { Message } from './dispatcher/Dispatcher'
import { Visualizer } from './visualizer/Visualizer'
import { VisualizerUI } from './visualizer_ui/VisualizerUI'
import './style-vis.scss'
import { ResizeManager } from './annotator_ui/ResizeManager'
import { mount, unmount } from 'svelte'

export class BratEditor implements AnnotationEditor {
  dispatcher: Dispatcher
  visualizer: Visualizer
  resizer: ResizeManager
  resizeObserver: ResizeObserver
  popover: AnnotationDetailPopOver

  public constructor (element: Element, ajax: DiamAjax, props: AnnotationEditorProperties) {
    const markupId = element.getAttribute('id')

    this.dispatcher = new Dispatcher()
    new Ajax(this.dispatcher, markupId, props.diamAjaxCallbackUrl)
    this.visualizer = new Visualizer(this.dispatcher, markupId, ajax)
    new VisualizerUI(this.dispatcher, ajax)
    new AnnotatorUI(this.dispatcher, this.visualizer, ajax)
    this.dispatcher.post('init')
    element.dispatcher = this.dispatcher
    element.visualizer = this.visualizer

    // Ensure that the visualizer is resized when the container element size changes, e.g. when
    // the sidebars are opened or closed.
    if (element.parentElement) {
      this.resizeObserver = new ResizeObserver(() => {
        // console.log(`resize: ${element.id} ${element.clientWidth} ${element.clientHeight}`)
        this.dispatcher.post('resize')
      })
      this.resizeObserver.observe(element.parentElement)
    }

    this.resizer = new ResizeManager(this.dispatcher, this.visualizer, ajax)

    this.popover = mount(AnnotationDetailPopOver, {
      target: document.body,
      props: {
        root: element,
        ajax
      }
    })
  }

  post (command: Message, data: any) : void {
    console.log(`post: ${command}`, data)
    this.dispatcher.post(command, data)
  }

  loadAnnotations (): void {
    console.log('loadAnnotations')
    this.dispatcher.post('loadAnnotations', [])
  }

  scrollTo (args: { offset: number, position?: string, pingRanges?: Offsets[] }): void {
    this.visualizer.scrollTo(args)
  }

  destroy (): void {
    console.log("Destroying BratEditor")
    if (this.popover) {
      unmount(this.popover)
    }
    if (this.resizeObserver) { 
      this.resizeObserver.disconnect()
    }
  }
}
