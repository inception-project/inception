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
import {FeatureX} from "../../../model/FeatureX";

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * SpanCreatedMessage: Message received by clients because a Span annotation has been created
 *
 * Attributes:
 * @spanId: The ID of the new Span
 * @begin: The character offset begin of the span
 * @end: The character offset end of the span
 * @color: The color of the Arc
 * @layerId: The ID of the layer the Arc belongs to
 * @features: List of AnnotationFeatures (FeatureX) that the Span has
 * @deprecated 
 */
export class SpanCreatedMessage
{
    spanId : number;
    begin : number;
    end : number;
    color: string;
    layerId: number;
    features : FeatureX[];
}