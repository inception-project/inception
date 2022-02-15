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
package de.tudarmstadt.ukp.inception.kb.factlinking.feature;

@Deprecated
public final class FactLinkingConstants
{
    public static final String SUBJECT_LINK = "de.tudarmstadt.ukp.inception.api.kb.type.FactSubjectLink";
    public static final String OBJECT_LINK = "de.tudarmstadt.ukp.inception.api.kb.type.FactObjectLink";
    public static final String QUALIFIER_LINK = "de.tudarmstadt.ukp.inception.api.kb.type.FactQualifierLink";

    public static final String SUBJECT_ROLE = "subject";
    public static final String OBJECT_ROLE = "object";

    public static final String FACT_LAYER = "de.tudarmstadt.ukp.inception.api.kb.type.Fact";

    // identifier here is a feature of the NamedEntity layer
    public static final String LINKED_LAYER_FEATURE = "identifier";
}
