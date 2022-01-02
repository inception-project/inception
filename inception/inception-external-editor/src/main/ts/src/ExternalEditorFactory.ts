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
import { AnnotationEditor, AnnotationEditorFactory, DiamClientFactory } from "@inception-project/inception-diam";
import { AnnotationEditorProperties } from "@inception-project/inception-diam/diam/AnnotationEditorProperties";

const PROP_EDITOR = "__editor__";

export class ExternalEditorFactory implements AnnotationEditorFactory {
  async getOrInitialize(element: string | HTMLElement, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    if (!(element instanceof HTMLElement)) {
      element = document.getElementById(element)
    }

    const iframe = element as HTMLIFrameElement;

    if (element[PROP_EDITOR] != null) {
      return element[PROP_EDITOR];
    }

    element[PROP_EDITOR] = await this.loadEditorResourcesIFrame(iframe, props).then(f => this.initEditorWithinIFrame(f, diam, props));
    return element[PROP_EDITOR];
  }

  loadEditorResourcesIFrame(iframe: HTMLIFrameElement, props: AnnotationEditorProperties): Promise<HTMLIFrameElement> {
    return new Promise(resolve => {
      const eventHandler = () => {
        iframe.removeEventListener('load', eventHandler);
        let allPromises: Promise<void>[] = [];
        allPromises.push(...props.stylesheetSources.map(src => this.loadStylesheet(iframe.contentDocument, src)));
        allPromises.push(...props.scriptSources.map(src => this.loadScript(iframe.contentDocument, src)));
        Promise.all(allPromises).then(() => { 
          console.info("All editor resources loaded");
          resolve(iframe);
        });
      };
      iframe.addEventListener('load', eventHandler);
    });
  }

  loadStylesheet(document: Document, styleSheetSource: string): Promise<void> {
    return new Promise(resolve => {
      var css = document.createElement("link");
      css.rel = "stylesheet";
      css.type = "text/css";
      css.href = styleSheetSource;
      css.onload = () => { 
        console.info(`Loaded stylesheet: ${styleSheetSource}`);
        css.onload = null;
        resolve();
      };
      document.getElementsByTagName('head')[0].appendChild(css);
    });
  }

  loadScript(document: Document, scriptSource: string): Promise<void> {
    return new Promise(resolve => {
      var script = document.createElement("script");
      script.type = "text/javascript";
      script.src = scriptSource;
      script.onload = () => { 
        console.info(`Loaded script: ${scriptSource}`);
        script.onload = null;
        resolve();
      };
      document.getElementsByTagName('head')[0].appendChild(script);
    });
  }

  initEditorWithinIFrame(iframe: HTMLIFrameElement, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    return new Promise(resolve => {
      const bodyElement = iframe.contentDocument.getElementsByTagName("body")[0];
      const editorFactory = (iframe.contentWindow as any).eval(props.editorFactory) as AnnotationEditorFactory;
      editorFactory.getOrInitialize(bodyElement, diam, props).then(editor => {
        console.info("Editor in IFrame initialized");
        resolve(editor);
      });
    });
  }

  destroy(element: string | HTMLElement): void {
    if (!(element instanceof HTMLElement)) {
      element = document.getElementById(element)
    }

    if (element[PROP_EDITOR] != null) {
      element[PROP_EDITOR].destroy();
      console.info('Destroyed editor in IFrame');
    }
  }
}