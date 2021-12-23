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
import {FeatureX} from "./FeatureX";

/**
 * Support Class representing an Arc annotation
 *
 * Attributes:
 * @id: The ID of the Arc
 * @sourceId: The ID of the source span annotation for the Arc
 * @targetId: The ID of the target span annotation for the Arc
 * @layerId: The ID of the layer the Arc belongs to
 * @features: List of annotation features (FeatureX) of the Arc
 * @color: Color of the Arc
 * @deprecated 
 */
export class Arc
{
    id : number;
    sourceId : number;
    targetId : number;
    layerId: number;
    features: FeatureX[];
    color : string;

    constructor(aId: number, aSourceId: number, aTargetId: number, aLayerId: number, aFeatures: FeatureX[], aColor: string)
    {
        this.id = aId;
        this.sourceId = aSourceId;
        this.targetId = aTargetId;
        this.layerId = aLayerId;
        this.features = aFeatures;
        this.color = aColor;
    }
}