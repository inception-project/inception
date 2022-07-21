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
// import { Ajax } from './ajax/Ajax'
// import { factory as diamAjaxFactory } from '@inception-project/inception-diam'
// import { AnnotatorUI } from './annotator_ui/AnnotatorUI'
// import { Dispatcher } from './dispatcher/Dispatcher'
// import { Visualizer } from './visualizer/Visualizer'
// import { VisualizerUI } from './visualizer_ui/VisualizerUI'

// declare let Wicket

// function brat (markupId: string, callbackUrl: string) {
//   console.debug('Setting up brat editor...')

//   const dispatcher = new Dispatcher()
//   const diamAjax = diamAjaxFactory().createAjaxClient(callbackUrl)
//   new Ajax(dispatcher, markupId, callbackUrl)
//   const visualizer = new Visualizer(dispatcher, markupId)
//   new VisualizerUI(dispatcher)
//   new AnnotatorUI(dispatcher, visualizer.svg, diamAjax)
//   dispatcher.post('init')
//   Wicket.$(markupId).dispatcher = dispatcher
//   Wicket.$(markupId).visualizer = visualizer
// }

// export = brat;

import { BratEditorFactory } from './BratEditorFactory'

const INSTANCE = new BratEditorFactory()

export function factory (): BratEditorFactory {
  return INSTANCE
}
