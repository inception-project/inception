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

import '@recogito/recogito-js/dist/recogito.min.css'
import { Recogito } from '@recogito/recogito-js';
import { DiamAjax } from "@inception-project/inception-diam/diam/Diam";

export interface AnnotationEditor {
  loadAnnotations() : void;
}

export class RecogitoEditor implements AnnotationEditor {
  private ajax : DiamAjax; 
  private callbackUrl : string;
  private recogito : Recogito;

  public constructor(element: HTMLElement, callbackUrl: string) {
    this.ajax = new DiamAjax(callbackUrl);
    this.callbackUrl = callbackUrl;

    this.recogito = new Recogito({
      content: element,
      disableEditor: true,
      mode: 'pre'
    });

    this.recogito.on('createAnnotation', annotation => this.createAnnotation(annotation));
    this.recogito.on('selectAnnotation', annotation => this.selectAnnotation(annotation));
  }

  public static getInstance(element: HTMLElement | string, callbackUrl: string) : RecogitoEditor {
    if (!(element instanceof HTMLElement)) {
      element = document.getElementById(element)
    }

    if (element['recogito'] != null) {
      return element['recogito'];
    }

    let instance : RecogitoEditor = new RecogitoEditor(element, callbackUrl);
    element['recogito'] = instance;
    return instance;
  }

  public static destroy(element: HTMLElement | string) {
    if (!(element instanceof HTMLElement)) {
      element = document.getElementById(element)
    }

    if (element['recogito'] != null) {
      element['recogito'].destroy(); 
      console.log('Destroyed RecogitoJS');
    }
  }

  public loadAnnotations() : void {
    this.recogito.loadAnnotations(this.callbackUrl);
  }

  private createAnnotation(annotation) : void {
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

    this.ajax.createSpanAnnotation([[begin, end]], text);
  }

  private selectAnnotation(annotation) : void {
    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    this.ajax.selectAnnotation(annotation.id.substring('1'));
  }
}