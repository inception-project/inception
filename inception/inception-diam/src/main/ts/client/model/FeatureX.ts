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

/**
 * Support Class representing Features for an annotation
 *
 * Each feature has a name and a @value. The @value can be of various types (any)
 *
 * Attributes:
 * @name: String representation of the features names
 * @value: Value of the feature. Can be of various types (any)
 **/
export class FeatureX
{
    name : string;
    value : any;

    constructor(aName : string, aValue : any)
    {
        this.name = aName;
        this.value = aValue;
    }
}