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
package de.tudarmstadt.ukp.inception.search.index.mtas.footprint;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.footprint.Footprint;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProvider;
import de.tudarmstadt.ukp.inception.search.index.mtas.MtasDocumentIndexFactory;

public class MtasDocumentIndexFootprintProvider
    implements FootprintProvider
{
    private final MtasDocumentIndexFactory mtasDocumentIndexFactory;

    public MtasDocumentIndexFootprintProvider(MtasDocumentIndexFactory aMtasDocumentIndexFactory)
    {
        mtasDocumentIndexFactory = aMtasDocumentIndexFactory;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        var indexDir = mtasDocumentIndexFactory.getIndexDir(aContext);
        return indexDir.exists() && indexDir.isDirectory() && indexDir.canRead();
    }

    @Override
    public List<Footprint> getFootprint(Project aProject)
    {
        try {
            var totalIndexSize = mtasDocumentIndexFactory.getIndexSize(aProject);
            return asList( //
                    new Footprint("Annotation index", totalIndexSize, "pink"));
        }
        catch (Exception e) {
            return emptyList();
        }
    }
}
