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
import { DiamAjax, Offsets, VID } from '@inception-project/inception-js-api';
import { DiamLoadAnnotationsOptions } from '@inception-project/inception-js-api/src/diam/DiamAjax';

declare const Wicket: any;

type SuccessHandler = (attrs, jqXHR, data, textStatus: string) => void;
type ErrorHandler = (attrs, jqXHR, errorMessage, textStatus: string) => void;

if (!document.hasOwnProperty('DIAM_TRANSPORT_BUFFER')) {
  document['DIAM_TRANSPORT_BUFFER'] = {};
}

const TRANSPORT_BUFFER: any = document['DIAM_TRANSPORT_BUFFER'];

export class DiamAjaxImpl implements DiamAjax {
  private ajaxEndpoint: string;

  constructor(ajaxEndpoint: string) {
    this.ajaxEndpoint = ajaxEndpoint;
  }

  selectAnnotation(id: string) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'selectAnnotation',
        id: id
      }
    });
  }

  createSpanAnnotation(offsets: Array<Offsets>, spanText: string) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'spanOpenDialog',
        offsets: JSON.stringify(offsets),
        spanText: spanText
      }
    });
  }

  createRelationAnnotation(originSpanId: VID, targetSpanId: VID) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'arcOpenDialog',
        originSpanId: originSpanId,
        targetSpanId: targetSpanId,
      }
    });
  }

  static newToken(): string {
    return btoa(String.fromCharCode(...window.crypto.getRandomValues(new Uint8Array(16))));
  }

  static storeResult(token: string, result: string) {
    TRANSPORT_BUFFER[token] = JSON.parse(result);
  }

  static clearResult(token: string): any {
    if (TRANSPORT_BUFFER.hasOwnProperty(token)) {
      const result = TRANSPORT_BUFFER[token];
      delete TRANSPORT_BUFFER[token];
      return result;
    }

    return undefined;
  }

  loadAnnotations(options?: DiamLoadAnnotationsOptions): Promise<any> {
    const token = DiamAjaxImpl.newToken();

    let params : Record<string, any> = {
      "action": 'loadAnnotations',
      "token": token
    };
    
    if (options) {
      if (options.format) {
        params.format = options.format;
      }
    }

    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        "m": "POST",
        "u": this.ajaxEndpoint,
        "ep": params,
        "sh": [() => {
          const result = DiamAjaxImpl.clearResult(token);
          if (result === undefined) {
            reject("Server did not place result into transport buffer");
            return;
          }

          resolve(result);
        }],
        "eh": [() => {
          DiamAjaxImpl.clearResult(token);

          reject("Unable to load annotation");
        }]
      });
    });
  }
}
