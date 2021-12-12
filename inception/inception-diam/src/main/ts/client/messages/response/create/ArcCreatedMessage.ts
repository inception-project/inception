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
 * ArcCreatedMessage: Message received by clients because an Arc annotation has been created
 *
 * Attributes:
 * @arcId: The ID of the new arc
 * @projectId: The ID of the project to which the new Arc belongs
 * @sourceId: The ID of the source annotation of the Arc
 * @targetId: The ID of the target annotation of the Arc
 * @color: The color of the Arc
 * @layerId: The ID of the layer the Arc belongs to
 * @features: List of AnnotationFeatures (FeatureX) that the Arc has
 **/
export class ArcCreatedMessage
{
    arcId : number;
    projectId: number;
    sourceId : number;
    targetId : number;
    color: string;
    layerId: number;
    features : FeatureX[];
}