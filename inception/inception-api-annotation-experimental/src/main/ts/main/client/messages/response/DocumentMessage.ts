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
import {Span} from "../../model/Span";
import {Arc} from "../../model/Arc";
import {Viewport} from "../../model/Viewport";

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * DocumentMessage: Message from server containing the data for the requested document
 *
 * Attributes:
 * @viewport: List of requested Viewports and their contents
 * @sourceDocumentId: The ID of the requested sourcedocument
 * @spans: List of Spans contained in the requested viewport for a certain document
 * @arcs: List of Arcs contained in the requested viewport for a certain document
 **/
export class DocumentMessage
{
    viewport : Viewport[];
    sourceDocumentId : number;
    spans : Span[];
    arcs : Arc[];
}