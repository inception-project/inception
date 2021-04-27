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
import {Annotation} from "./annotation/Annotation";

export type ServerMessage =
    ServerMessageNewAnnotation
    | ServerMessageDeletedAnnotation
    | ServerMessageNewClientConnected
    | ServerMessageSelectedAnnotation
    | ServerMessageNewDocumentForClient;


export type ServerMessageSelectedAnnotation =
{
    type : "selectedAnnotationForClient";
    annotation : Annotation;
}
export type ServerMessageNewAnnotation =
{
    type : "newAnnotationForClient";
    annotation : Annotation;
}

export type ServerMessageDeletedAnnotation =
{
    type : "deletedAnnotationForClient";
    id : string;
}

export type ServerMessageNewClientConnected =
{
    type : "newConnectedClientForClient";
}

export type ServerMessageNewDocumentForClient =
{
    type : "newDocumentForClient";
    document : JSON
}


//Client message types to server

export type ClientMessage =
    ClientMessageConnectedClient
    | ClientMessageDisconnectedClient
    | ClientMessageCreatedAnnotation
    | ClientMessageDeleteAnnotation
    | ClientMessageNewDocument;

export type ClientMessageConnectedClient =
{
    type : "clientConnected";
    id : number;
}

export type ClientMessageDisconnectedClient =
{
    type : "clientDisconnected";
    id : number;
}

export type ClientMessageCreatedAnnotation =
{
    type : "newAnnotationByClient";
    clientId : number;
    annotation : Annotation;
}

export type ClientMessageDeleteAnnotation =
{
    type : "deletedAnnotationByClient";
    clientId : number;
    annotationId : string;
}

export type ClientMessageNewDocument =
{
    type : "newDocumentRequestedByClient";
    clientId : number;
}
