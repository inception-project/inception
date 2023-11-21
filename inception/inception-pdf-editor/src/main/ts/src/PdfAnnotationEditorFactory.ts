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
import type { AnnotationEditorFactory, AnnotationEditorProperties, DiamClientFactory } from "@inception-project/inception-js-api"
import { PdfAnnotationEditor } from "./PdfAnnotationEditor"

const PROP_EDITOR = "__editor__";

export class PdfAnnotationEditorFactory implements AnnotationEditorFactory {
  public async getOrInitialize(element: Node, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<PdfAnnotationEditor> {
    if (element[PROP_EDITOR] != null) {
      return element[PROP_EDITOR];
    }

    const ajax = diam.createAjaxClient(props.diamAjaxCallbackUrl);

    let targetElement = element as Element;
    element[PROP_EDITOR] = new PdfAnnotationEditor(targetElement, ajax);
    return element[PROP_EDITOR];
  }

  public destroy(element: Node) {
    if (element[PROP_EDITOR] != null) {
      element[PROP_EDITOR].destroy();
      console.log('Destroyed editor');
    }
  }
}
