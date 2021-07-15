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
export class Relation
{
    relationId : number;
    vidGovernor : number;
    vidDependent : number;
    color : string;
    dependencyType : string;
    flavor : string;

    constructor(aRelationId: number, aVidGovernor: number, aVidDependent: number, aColor: string, aDependencyType: string, aFlavor: string)
    {
        this.relationId = aRelationId;
        this.vidGovernor = aVidGovernor;
        this.vidDependent = aVidDependent;
        this.color = aColor;
        this.dependencyType = aDependencyType;
        this.flavor = aFlavor;
    }

}