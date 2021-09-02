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
import {Viewport} from "./model/Viewport";
import {FeatureX} from "./model/FeatureX";
import {AdviceMessage} from "./messages/response/AdviceMessage";
import {DocumentMessage} from "./messages/response/DocumentMessage";
import {UpdateFeaturesMessage} from "./messages/response/UpdateFeaturesMessage";
import {DeleteAnnotationMessage} from "./messages/response/DeleteAnnotationMessage";
import {SpanCreatedMessage} from "./messages/response/create/SpanCreatedMessage";
import {ArcCreatedMessage} from "./messages/response/create/ArcCreatedMessage";

export interface AnnotationExperienceAPI
{
    unsubscribe(aChannel: string);

    disconnect();

    requestDocument(aAnnotatorName: string, aProjectId: number,
                                 aDocumentId: number, aViewport: Viewport[]);

    requestUpdateFeatures(aAnnotatorName: string, aProjectId: number,
                                aDocumentId: number, aAnnotationId: number, aNewFeature: FeatureX[])

    requestCreateSpan(aAnnotatorName: string, aProjectId: number,
                                aDocumentId: number, aBegin: number, aEnd: number, aLayer: number);

    requestDeleteAnnotation(aAnnotatorName: string,  aProjectId: number,
                                aDocumentId: number, aAnnotationId: number, aLayer: number);

    requestCreateArc(aAnnotatorName: string, aProjectId: number, aDocumentId: number, aSourceId : string, aTargetId : string, aLayer: number)

    onDocument(aMessage: DocumentMessage);

    onSpanCreate(aMessage: SpanCreatedMessage);

    onArcCreate(aMessage: ArcCreatedMessage);

    onFeaturesUpdate(aMessage: UpdateFeaturesMessage);

    onAnnotationDelete(aMessage: DeleteAnnotationMessage);

    onError(aMessage: AdviceMessage);
}