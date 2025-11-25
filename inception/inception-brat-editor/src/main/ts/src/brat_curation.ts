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
import { Ajax } from './ajax/Ajax'
import { INSTANCE as Util } from './util/Util'
import { CurationMod } from './curation/CurationMod'
import { factory as diamAjaxFactory } from '@inception-project/inception-diam'
import AnnotationDetailPopOver from '@inception-project/inception-js-api/src/widget/AnnotationDetailPopOver.svelte'
import './style-vis.scss'
import { mount } from 'svelte'

declare let Wicket

function brat (markupId: string, controllerCallbackUrl: string, collCallbackUrl: string, docCallbackUrl: string) {
  const diamAjax = diamAjaxFactory().createAjaxClient(controllerCallbackUrl)
  Util.embedByURL(markupId, diamAjax, collCallbackUrl, docCallbackUrl,
    function (dispatcher) {
      // eslint-disable-next-line no-new
      new Ajax(dispatcher, markupId, controllerCallbackUrl)
      // eslint-disable-next-line no-new
      new CurationMod(dispatcher, diamAjax)

      const element = Wicket.$(markupId)

      // eslint-disable-next-line no-new
      mount(AnnotationDetailPopOver, {
        target: document.body,
        props: {
          root: element,
          ajax: diamAjax
        }
      })

      Wicket.$(markupId).dispatcher = dispatcher
    })
}

export = brat;
