
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
import "./annotatorjs/css/annotator.css";

import { AnnotationEditor, CompactAnnotatedText, DiamAjax } from "@inception-project/inception-js-api";
import { Annotator } from './annotatorjs/src/annotator';
import { Store } from './annotatorjs/src/plugin/store';
import { DiamStore } from "./annotatorjs/src/plugin/diamstore";

export class AnnotatorJsEditor implements AnnotationEditor {
  private ajax: DiamAjax;
  private store: DiamStore;
  private annotatorJs: Annotator;

  public constructor(element: Element, ajax: DiamAjax) {
//    this.storeCallbackUrl = storeCallbackUrl;

    this.store = new DiamStore(element, ajax);
    this.annotatorJs = new Annotator(element, {});
    this.annotatorJs.addPlugin('Store', this.store);
    // this.annotatorJs.addPlugin('Store', new Store(element, {
    //   prefix: null,
    //   emulateJSON: true,
    //   emulateHTTP: true,
    //   urls: {
    //     read: this.storeCallbackUrl,
    //     create: this.storeCallbackUrl,
    //     update: this.storeCallbackUrl,
    //     destroy: this.storeCallbackUrl,
    //     search: this.storeCallbackUrl,
    //     select: this.storeCallbackUrl
    //   }
    // }));
  }

  loadAnnotations(): void {
    this.store.loadAnnotations();
  }

  public destroy(): void {
    this.annotatorJs.destroy();
  }
}