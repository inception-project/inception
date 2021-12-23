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
import {FeatureX} from "../../model/FeatureX";

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * UpdateFeatureRequest: Request forwarded to the Server to update a certain feature of an annotation
 * Following parameters are required for retrieving the CAS: @annotatorName, @projectId, @sourceDocumentId
 *
 * Attributes:
 * @annotatorName: String representation of the name of the annotator the annotation will belong to
 * @projectId: The ID of the project the annotation will belong to
 * @sourceDocumentId: The ID of the Sourcedocument the annotation belongs to
 * @annotationId: The ID of the annotation for which a feature value shall be changed
 * @layerId: The ID of the layer the annotation belongs to
 * @feature: The feature for which a @value shall be assigned
 * @value: The new value for the @feature
 * @deprecated 
 */
export class UpdateFeatureRequest
{
    annotatorName: string;
    projectId: number;
    sourceDocumentId: number;
    annotationId: number;
    layerId: number;
    feature: FeatureX;
    value: any;

    constructor(aAnnotatorName: string, aProjectId: number, aSourceDocumentId: number, aAnnotationId: number, aLayerId: number, aFeature: FeatureX, aValue: any)
    {
        this.annotatorName = aAnnotatorName;
        this.projectId = aProjectId;
        this.sourceDocumentId = aSourceDocumentId;
        this.annotationId = aAnnotationId;
        this.layerId = aLayerId;
        this.feature = aFeature;
        this.value = aValue;
    }

}