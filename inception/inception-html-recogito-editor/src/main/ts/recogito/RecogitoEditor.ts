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
import type { AnnotationEditor, DiamAjax } from "@inception-project/inception-diam";

const FORMAT_RECOGITO_JS = "RecogitoJs";

export class RecogitoEditor implements AnnotationEditor {
  private ajax: DiamAjax;
  private recogito: Recogito;

  public constructor(element: HTMLElement, ajax: DiamAjax) {
    this.ajax = ajax;

    this.recogito = new Recogito({
      content: element,
      disableEditor: true,
      mode: 'pre'
    });

    this.recogito.on('createAnnotation', annotation => this.createAnnotation(annotation));
    this.recogito.on('selectAnnotation', annotation => this.selectAnnotation(annotation));

    this.loadAnnotations();
  }

  public loadAnnotations(): void {
    this.ajax.loadAnnotations(FORMAT_RECOGITO_JS).then(a => this.recogito.setAnnotations(a));
  }

  public destroy(): void {
    this.recogito.destroy();
  }

  private createAnnotation(annotation): void {
    let target = annotation.target;
    let text: string, begin: number, end: number;
    for (let i = 0; i < target.selector.length; i++) {
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

  private selectAnnotation(annotation): void {
    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    this.ajax.selectAnnotation(annotation.id.substring('1'));
  }
}