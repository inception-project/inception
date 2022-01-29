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
import { AnnotationEditor, AnnotationEditorFactory, AnnotationEditorProperties, DiamClientFactory } from "@inception-project/inception-js-api";

const PROP_EDITOR = "__editor__";

export class ExternalEditorFactory implements AnnotationEditorFactory {
  async getOrInitialize(element: Node, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    const iframe = element as HTMLIFrameElement;

    if (element[PROP_EDITOR] != null) {
      return element[PROP_EDITOR];
    }

    element[PROP_EDITOR] = await this.loadIFrameContent(iframe)
      .then(f => this.loadEditorResources(f, props))
      .then(f => {
        if (this.isDocumentJavascriptCapable(iframe.contentDocument)) {
            // On HTML documents provide the body element as target to the editor
            return this.initEditor(iframe.contentWindow,
            iframe.contentDocument.getElementsByTagName("body")[0], diam, props);
        }

        // On XML documents, provide the document root as target to the editor
        return this.initEditor(window, f.contentDocument, diam, props);
      });
    return element[PROP_EDITOR];
  }

  loadIFrameContent(iframe: HTMLIFrameElement): Promise<HTMLIFrameElement> {
    return new Promise(resolve => {
      const eventHandler = () => {
        iframe.removeEventListener('load', eventHandler);
        resolve(iframe);
      };
      iframe.addEventListener('load', eventHandler);
    });
  }

  loadEditorResources(iframe: HTMLIFrameElement, props: AnnotationEditorProperties): Promise<HTMLIFrameElement> {
    return new Promise(resolve => {
      let target = this.isDocumentJavascriptCapable(iframe.contentDocument)
        ? iframe.contentDocument : document;
      let allPromises: Promise<void>[] = [];
      if (this.isDocumentStylesheetCapable(iframe.contentDocument)) {
        allPromises.push(...props.stylesheetSources.map(src => this.loadStylesheet(target, src)));
      }
      allPromises.push(...props.scriptSources.map(src => this.loadScript(target, src)));

      Promise.all(allPromises).then(() => {
        console.info(`${allPromises.length} editor resources loaded`);
        resolve(iframe);
      });
    });
  }

  initEditor(contextWindow: Window, targetElement: HTMLElement | Document, diam: DiamClientFactory, props: AnnotationEditorProperties): Promise<AnnotationEditor> {
    return new Promise(resolve => {
      const editorFactory = (contextWindow as any).eval(props.editorFactory) as AnnotationEditorFactory;
      editorFactory.getOrInitialize(targetElement, diam, props).then(editor => {
        console.info("Editor in HTML IFrame initialized");
        resolve(editor);
      });
    });
  }

  isDocumentJavascriptCapable(document: Document): boolean {
    return document instanceof HTMLDocument || document.documentElement.constructor.name === "HTMLHtmlElement";
  }

  isDocumentStylesheetCapable(document: Document): boolean {
    // We could theoretically also inject an XML-stylesheet processing instruction into the 
    // IFrame, but probably we could not wait for the stylesheet to load as we do in HTML
    return document instanceof HTMLDocument || document.documentElement.constructor.name === "HTMLHtmlElement";
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

  destroy(element: Node): void {
    if (element[PROP_EDITOR] != null) {
      element[PROP_EDITOR].destroy();
      console.info('Destroyed editor in IFrame');
    }
  }
}