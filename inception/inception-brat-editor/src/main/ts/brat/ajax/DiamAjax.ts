import { OffsetsList } from "../visualizer/SourceData";
import { Span } from "../visualizer/Span";

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
declare const Wicket: any;

export class DiamAjax {
  private ajaxEndpoint: string;

  constructor(ajaxEndpoint: string) {
    this.ajaxEndpoint = ajaxEndpoint;
  }

  selectSpanAnnotation(offsets: OffsetsList, span: Span, id?) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'spanOpenDialog',
        offsets: JSON.stringify(offsets),
        id: id,
        type: span.type,
        spanText: span.text
      }
    });
  }

  selectArcAnnotation(originSpanId, originType, targetSpanId, targetType, arcType, arcId) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'arcOpenDialog',
        arcId: arcId,
        arcType: arcType,
        originSpanId: originSpanId,
        originType: originType,
        targetSpanId: targetSpanId,
        targetType: targetType
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

  createRelationAnnotation(originSpanId, originType, targetSpanId, targetType) {
    Wicket.Ajax.ajax({
      "m": "POST",
      "u": this.ajaxEndpoint,
      "ep": {
        action: 'arcOpenDialog',
        originSpanId: originSpanId,
        originType: originType,
        targetSpanId: targetSpanId,
        targetType: targetType
      }
    });
  }
}

