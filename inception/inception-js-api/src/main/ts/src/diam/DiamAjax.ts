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
import { LazyDetail } from '@inception-project/inception-js-api/src/model/LazyDetail'
import { Annotation, LazyDetailGroup, Offsets, VID } from '../model'

export type DiamLoadAnnotationsOptions = {
  format?: string,
  range?: Offsets,
  includeText?: boolean
  clipSpans?: boolean
}

export type DiamSelectAnnotationOptions = {
  scrollTo?: boolean
}

export interface DiamAjax {
  selectAnnotation(id: VID, options?: DiamSelectAnnotationOptions): void;

  /**
   * Scroll to the given annotation or offset. If both are given, the offset is used. Offsets
   * are relative to the entire document, not to the current viewport.
   */
  scrollTo(args: { id?: VID, offset?: Offsets }): void;

  /**
   * Delete the annotation with the given VID.
   *
   * @param id the VID of the annotation to delete.
   */
  deleteAnnotation(id: VID): void;

  /**
   * Create a new span annotation at the given location.
   *
   * @param offsets the offsets of the annotation.
   *
   * NOTE: Currently only a single element is supported in the offsets array.
   */
  createSpanAnnotation(offsets: Array<Offsets>, spanText?: string): void;

  /**
   * Move a new span annotation to a new location.
   *
   * @param offsets the offsets of the annotation.
   *
   * NOTE: Currently only a single element is supported in the offsets array.
   */
  moveSpanAnnotation(id: VID, offsets: Array<Offsets>): void;

  createRelationAnnotation(originSpanId: VID, targetSpanId: VID): void;

  loadAnnotations(options?: DiamLoadAnnotationsOptions): Promise<any>;

  /**
   * Loads the lazy details for the given annotation
   *
   * @param ann either the VID or the annotation itself
   * @param layerId the layer ID of the annnotation if the annotation is specified as a VID
   */
  loadLazyDetails(ann: VID | Annotation, layerId?: number): Promise<LazyDetailGroup[]>;

  loadPreferences (key: string): Promise<any>;

  savePreferences (key: string, data: Record<string, any>): Promise<void>;

  triggerExtensionAction(id: VID): void;

  openContextMenu(id: VID, evt: MouseEvent): void;
}
