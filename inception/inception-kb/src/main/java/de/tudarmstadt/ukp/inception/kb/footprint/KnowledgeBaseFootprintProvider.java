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
package de.tudarmstadt.ukp.inception.kb.footprint;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.project.api.footprint.Footprint;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProvider;

public class KnowledgeBaseFootprintProvider
    implements FootprintProvider
{
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseFootprintProvider(KnowledgeBaseService aKnowledgeBaseService)
    {
        knowledgeBaseService = aKnowledgeBaseService;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return !knowledgeBaseService.getKnowledgeBases(aContext).isEmpty();
    }

    @Override
    public List<Footprint> getFootprint(Project aProject)
    {
        try {
            var totalIndexSize = knowledgeBaseService.getKnowledgeBases(aProject).stream() //
                    .mapToLong(kb -> knowledgeBaseService.getIndexSize(kb)) //
                    .sum();

            var totalDataSize = knowledgeBaseService.getKnowledgeBases(aProject).stream() //
                    .mapToLong(kb -> knowledgeBaseService.getRepositorySize(kb)) //
                    .sum();

            return asList( //
                    new Footprint("KB index", totalIndexSize, "blue"), //
                    new Footprint("KB data", totalDataSize, "lightblue"));
        }
        catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
