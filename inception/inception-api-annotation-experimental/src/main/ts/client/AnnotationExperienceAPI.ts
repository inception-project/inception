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
import {NewDocumentResponse} from "./messages/response/NewDocumentResponse";
import {NewViewportResponse} from "./messages/response/NewViewportResponse";
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {SelectSpanResponse} from "./messages/response/span/SelectSpanResponse";
import {UpdateSpanResponse} from "./messages/response/span/UpdateSpanResponse";
import {CreateSpanResponse} from "./messages/response/span/CreateSpanResponse";
import {DeleteSpanResponse} from "./messages/response/span/DeleteSpanResponse";
import {SelectRelationResponse} from "./messages/response/relation/SelectRelationResponse";
import {UpdateRelationResponse} from "./messages/response/relation/UpdateRelationResponse";
import {CreateRelationResponse} from "./messages/response/relation/CreateRelationResponse";
import {Span} from "./model/Span";
import {Relation} from "./model/Relation";
import {DeleteRelationResponse} from "./messages/response/relation/DeleteRelationResponse";
import {AllRelationResponse} from "./messages/response/relation/AllRelationResponse";
import {AllSpanResponse} from "./messages/response/span/AllSpanResponse";

export interface AnnotationExperienceAPI
{
    //Text and annotations
    text: string[];
    spans: Span[];
    selectedSpan: Span;

    relations: Relation[];
    selectedRelation: Relation;

    //Viewport
    viewport: number[][];

    unsubscribe(aChannel: string);

    disconnect();


    requestNewDocumentFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                 aViewport: number[][]);

    requestNewViewportFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                 aDocumentId: number, aViewport: number[][]);

    requestSelectSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number);

    requestUpdateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number, aNewType: string)

    requestCreateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aBegin: number, aEnd: number, aType: string, aFeature: string);

    requestDeleteSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number);

    requestSelectRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number, aRelationAddress: number);

    requestUpdateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number, aRelationAddress: number, aNewFlavor: string, aNewRelation: string)

    requestCreateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number, aDocumentId: number, aGovernorId : number, aDependentId : number, aDependencyType : string, aFlavor : string)

    requestDeleteRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number, aRelationAddress: number);

    requestAllSpansFromServer(aClientName: string, aUserName: string, aProjectId: number,
                               aDocumentId: number);

    requestAllRelationsFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                  aDocumentId: number);

    onNewDocument(aMessage: NewDocumentResponse);

    onNewViewport(aMessage: NewViewportResponse);

    onSpanSelect(aMessage: SelectSpanResponse);

    onSpanUpdate(aMessage: UpdateSpanResponse);

    onSpanCreate(aMessage: CreateSpanResponse);

    onSpanDelete(aMessage: DeleteSpanResponse);

    onRelationSelect(aMessage: SelectRelationResponse);

    onRelationDelete(aMessage: DeleteRelationResponse);

    onRelationUpdate(aMessage: UpdateRelationResponse);

    onRelationCreate(aMessage: CreateRelationResponse);

    onAllSpans(aMessage: AllSpanResponse);

    onAllRelations(aMessage: AllRelationResponse);

    onError(aMessage: ErrorMessage);
}