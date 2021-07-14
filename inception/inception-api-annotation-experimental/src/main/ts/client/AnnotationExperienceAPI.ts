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
import {SelectSpanResponse} from "./messages/response/SelectSpanResponse";
import {UpdateSpanResponse} from "./messages/response/UpdateSpanResponse";
import {CreateSpanResponse} from "./messages/response/CreateSpanResponse";
import {DeleteSpanResponse} from "./messages/response/DeleteSpanResponse";
import {SelectRelationResponse} from "./messages/response/SelectRelationResponse";
import {UpdateRelationResponse} from "./messages/response/UpdateRelationResponse";
import {CreateRelationResponse} from "./messages/response/CreateRelationResponse";

export interface AnnotationExperienceAPI {

    unsubscribe(aChannel: string);

    disconnect();


    requestNewDocumentFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                 aDocumentId: number, aViewportType: string, aViewport: number[][],
                                 aRecommenderEnabled: boolean);

    requestNewViewportFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                 aDocumentId: number, aViewportType: string, aViewport: number[][],
                                 aRecommenderEnabled: boolean);

    requestSelectSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number);

    requestUpdateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number, aNewType: string)

    requestCreateSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aBegin: number, aEnd: number, aType: string, aFeature: string);

    requestDeleteSpanFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                aDocumentId: number, aSpanAddress: number);

    requestSelectRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number);

    requestUpdateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number)

    requestCreateRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number);

    requestDeleteRelationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                    aDocumentId: number);

    requestSaveWordAlignment(aClientName: string, aUserName: string, aProjectId: number, sentence: number, alignments: string)

    onNewDocument(aMessage: NewDocumentResponse);

    onNewViewport(aMessage: NewViewportResponse);

    onSpanSelect(aMessage: SelectSpanResponse);

    onSpanUpdate(aMessage: UpdateSpanResponse);

    onSpanCreate(aMessage: CreateSpanResponse);

    onSpanDelete(aMessage: DeleteSpanResponse);

    onRelationSelect(aMessage: SelectRelationResponse);

    onRelationUpdate(aMessage: UpdateRelationResponse);

    onRelationCreate(aMessage: CreateRelationResponse);

    onError(aMessage: ErrorMessage);
}