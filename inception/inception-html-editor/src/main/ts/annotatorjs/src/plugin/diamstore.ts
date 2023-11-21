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
import { CompactAnnotatedText, CompactSpan, DiamAjax } from '@inception-project/inception-js-api';
import { textChangeRangeIsUnchanged } from 'typescript';
import { Plugin, Annotator } from '../annotator';

interface Range {
  start: string;
  startOffset: number;
  end: string;
  endOffset: number;
}

interface Annotation {
  id: string;
  ranges: Array<Range>;
  quote?: string;
  text: string;
  color: string;
}

export class DiamStore extends Plugin {
  annotator: Annotator; // Annotator injects itself here when the plugin is registered

  private ajax: DiamAjax;

  constructor(element: any, diamAjax: DiamAjax) {
    super(element, {});
    this.ajax = diamAjax;

    // The store listens for the following events published by the Annotator.
    // - annotationCreated: A new annotation has been created.
    // - annotationUpdated: An annotation has been updated.
    // - annotationDeleted: An annotation has been deleted.
    Object.assign(this.events, {
      'annotationCreated': 'annotationCreated',
      // 'annotationDeleted': 'annotationDeleted',
      // 'annotationUpdated': 'annotationUpdated',
      'annotationSelected': 'annotationSelected'
    });

    this.loadAnnotations = this.loadAnnotations.bind(this);

    this.addEvents();

    this.loadAnnotations();
  }

  pluginInit() {
    // Nothing to do for now
  }

  annotationCreated(annotation: Annotation) {
    this.ajax.createSpanAnnotation(
      [[annotation.ranges[0].startOffset, annotation.ranges[0].endOffset]], 
      annotation.quote);
  }

  annotationSelected(annotation: Annotation) {
    this.ajax.selectAnnotation(annotation.id);
  }

  private compactSpansToAnnotation(spans: Array<CompactSpan>): Array<Annotation> {
    return spans.map(span => {
      return {
        id: span[0] as string,
        text: span[2].l || "",
        quote: "",
        color: span[2].c || "#000",
        ranges: [{ start: "", startOffset: span[1][0][0], end: "", endOffset: span[1][0][1] }]
      }
    });
  }

  loadAnnotations() {
    let annotations = [];
    this.ajax.loadAnnotations().then((doc: CompactAnnotatedText) => {
      if (doc.spans) {
        annotations.push(...this.compactSpansToAnnotation(doc.spans));
      }

      console.info(`Loaded ${annotations.length} annotations from server`);

      // FIXME: Clearing and re-rendering all the annotations DOES OBVIOUSLY NOT SCALE!
      this.annotator.clearAnnotations();
      this.annotator.loadAnnotations(annotations);
    });
  }
}