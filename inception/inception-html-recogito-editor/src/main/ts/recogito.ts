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

import { Recogito } from '@recogito/recogito-js';

import '@recogito/recogito-js/dist/recogito.min.css'
import { DiamAjax } from "@inception-project/inception-diam/client/DiamAjax";

function recogito(markupId: string, callbackUrl: string) {
  var ajax = new DiamAjax(callbackUrl);

  var r = new Recogito({
    content: document.getElementById(markupId),
    disableEditor: true,
    mode: 'pre'
  });

  r.on('createAnnotation', annotation => { 
    let target = annotation.target;
    let text: string, begin: number, end: number;
    for (let i = 0; i < target.selector.length; i ++) {
      if (target.selector[i].type === "TextQuoteSelector") {
        text = target.selector[i].exact;
      }
      if (target.selector[i].type === "TextPositionSelector") {
        begin = target.selector[i].start;
        end = target.selector[i].end;
      }
    }

    ajax.createSpanAnnotation([[begin, end]], text);
  });
  r.on('selectAnnotation', annotation => { 
    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    ajax.selectAnnotation(annotation.id.substring('1'));
  });

  r.loadAnnotations(callbackUrl);

  document.getElementById(markupId)['recogito'] = r;
}

export = recogito;