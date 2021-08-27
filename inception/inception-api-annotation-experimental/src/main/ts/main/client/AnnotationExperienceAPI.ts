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
import {Span} from "./model/Span";
import {Arc} from "./model/Arc";
import {DocumentResponse} from "./messages/response/DocumentResponse";
import {SelectSpanResponse} from "./messages/response/span/SelectSpanResponse";
import {UpdateSpanMessage} from "./messages/response/span/UpdateSpanMessage";
import {CreateSpanMessage} from "./messages/response/span/CreateSpanMessage";
import {DeleteSpanMessage} from "./messages/response/span/DeleteSpanMessage";
import {SelectArcResponse} from "./messages/response/arc/SelectArcResponse";
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {UpdateArcMessage} from "./messages/response/arc/UpdateArcMessage";
import {DeleteArcMessage} from "./messages/response/arc/DeleteArcMessage";
import {CreateArcMessage} from "./messages/response/arc/CreateArcMessage";

export interface AnnotationExperienceAPI
{
    //Text and annotations
    spans: Span[];
    selectedSpan: Span;

    arcs: Arc[];
    selectedArc: Arc;

    //Viewport
    viewport: Viewport;

    unsubscribe(aChannel: string);

    disconnect();


    requestDocument(aClientName: string, aProjectId: number,
                                 aDocumentId: number, aViewport: Viewport);

    requestSelectSpan(aClientName: string, aProjectId: number,
                                aDocumentId: number, aSpanId: string);

    requestUpdateSpan(aClientName: string, aProjectId: number,
                                aDocumentId: number, aSpanId: string, aNewFeature: FeatureX[])

    requestCreateSpan(aClientName: string, aProjectId: number,
                                aDocumentId: number, aBegin: number, aEnd: number, aLayer: string);

    requestDeleteSpan(aClientName: string,  aProjectId: number,
                                aDocumentId: number, aSpanId: string, aLayer: string);

    requestSelectArc(aClientName: string, aProjectId: number,
                                    aDocumentId: number, aArcId: string);

    requestUpdateArc(aClientName: string, aProjectId: number,
                                    aDocumentId: number, aArcId: string, aNewFeature: FeatureX[])

    requestCreateArc(aClientName: string, aProjectId: number, aDocumentId: number, aSourceId : string, aTargetId : string, aLayer: string)

    requestDeleteArc(aClientName: string, aProjectId: number,
                                    aDocumentId: number, aArcId: string, aLayer: string);

    onDocument(aMessage: DocumentResponse);

    onSpanSelect(aMessage: SelectSpanResponse);

    onSpanUpdate(aMessage: UpdateSpanMessage);

    onSpanCreate(aMessage: CreateSpanMessage);

    onSpanDelete(aMessage: DeleteSpanMessage);

    onArcSelect(aMessage: SelectArcResponse);

    onArcDelete(aMessage: DeleteArcMessage);

    onArcUpdate(aMessage: UpdateArcMessage);

    onArcCreate(aMessage: CreateArcMessage);

    onError(aMessage: ErrorMessage);
}