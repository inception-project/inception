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
import { OffsetsList } from "./model/Annotation";

declare const Wicket: any;

type SuccessHandler = (attrs, jqXHR, data, textStatus: string) => void;
type ErrorHandler = (attrs, jqXHR, errorMessage, textStatus: string) => void;

if (!document.hasOwnProperty('DIAM_TRANSPORT_BUFFER')) {
  document['DIAM_TRANSPORT_BUFFER'] = {};
}

const TRANSPORT_BUFFER: any = document['DIAM_TRANSPORT_BUFFER'];

export class DiamAjax {
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

  createSpanAnnotation(offsets: OffsetsList, spanText: string) {
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

  createRelationAnnotation(originSpanId, targetSpanId) {
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

  loadAnnotations(format: string): Promise<any> {
    const token = DiamAjax.newToken();
    return new Promise((resolve, reject) => {
      Wicket.Ajax.ajax({
        "m": "POST",
        "u": this.ajaxEndpoint,
        "ep": {
          "action": 'loadAnnotations',
          "token": token,
          "format": format
        },
        "sh": [() => {
          const result = DiamAjax.clearResult(token);
          if (result !== undefined) {
            resolve(result);
          }

          reject("Server did not place result into transport buffer");
        }],
        "eh": [() => {
          DiamAjax.clearResult(token);

          reject("Unable to load annotation");
        }]
      });
    });
  }
}
