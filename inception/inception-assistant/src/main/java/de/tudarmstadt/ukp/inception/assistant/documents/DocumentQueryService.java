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
package de.tudarmstadt.ukp.inception.assistant.documents;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface DocumentQueryService
{
    // Search fields
    final String FIELD_EMBEDDING = "field";
    final String FIELD_RANGE = "range";

    // Stored fields
    final String FIELD_SOURCE_DOC_COMPLETE = "sourceDocComplete";
    final String FIELD_SOURCE_DOC_ID = "sourceDoc";
    final String FIELD_SECTION = "section";
    final String FIELD_TEXT = "text";
    final String FIELD_BEGIN = "begin";
    final String FIELD_END = "end";

    List<Chunk> query(Project aProject, String aQuery, int aTopN, double aScoreThreshold);

    void rebuildIndexAsync(Project aProject);
}
