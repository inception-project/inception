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
import {SelectAnnotationResponse} from "./messages/response/SelectAnnotationResponse";
import {UpdateAnnotationResponse} from "./messages/response/UpdateAnnotationResponse";
import {ErrorMessage} from "./messages/response/ErrorMessage";
import {CreateAnnotationResponse} from "./messages/response/CreateAnnotationResponse";

export interface AnnotationExperienceAPI {

    //TODO Type safety

    unsubscribe(aChannel: string);

    disconnect();


    requestNewDocumentFromServer(aClientName : string, aUserName : string, aProjectId : number,
                                 aDocumentId : number, aViewportType : string, aViewport : number[][],
                                 aRecommenderEnabled : boolean);

    requestNewViewportFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                 aDocumentId: number, aViewportType: string, aViewport: number[][],
                                 aRecommenderEnabled: boolean);

    requestSelectAnnotationFromServer(clientName : string, userName : string, projectId : number,
                                      documentId : number, annotationAddress : number);

    requestUpdateAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                      aDocumentId : number, aAnnotationAddress: number, aNewType: string)

    requestCreateAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                      aDocumentId: number, aBegin: number, aEnd: number);

    requestDeleteAnnotationFromServer(aClientName: string, aUserName: string, aProjectId: number,
                                      aDocumentId: number, aAnnotationAddress: number);

    onNewDocument(aMessage: NewDocumentResponse);

    onNewViewport(aMessage: NewViewportResponse);

    onAnnotationSelect(aMessage: SelectAnnotationResponse);

    onAnnotationUpdate(aMessage: UpdateAnnotationResponse);

    onAnnotationCreate(aMessage: CreateAnnotationResponse);

    onError(aMessage: ErrorMessage);
}