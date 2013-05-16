/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A model for curation container comprises of {@link CurationSegmentForSourceDocument}, {@link SourceDocument},
 * and {@link Project}
 * @author  Andreas Straninger
 * @author  Seid Muhie Yimam
 *
 */
public class CurationContainer
    implements Serializable
{
    private static final long serialVersionUID = -6632707037285383353L;

    private Map<Integer, CurationSegmentForSourceDocument> curationSegmentByBegin = new HashMap<Integer, CurationSegmentForSourceDocument>();

    private SourceDocument sourceDocument;

    private Project project;

    public List<CurationSegmentForSourceDocument> getCurationSegments()
    {
        LinkedList<Integer> segmentsBegin = new LinkedList<Integer>(curationSegmentByBegin.keySet());
        Collections.sort(segmentsBegin);
        List<CurationSegmentForSourceDocument> curationSegments = new LinkedList<CurationSegmentForSourceDocument>();
        for (Integer begin : segmentsBegin) {
            curationSegments.add(curationSegmentByBegin.get(begin));
        }
        return curationSegments;
    }

    public Map<Integer, CurationSegmentForSourceDocument> getCurationSegmentByBegin()
    {
        return curationSegmentByBegin;
    }

    public void setCurationSegmentByBegin(Map<Integer, CurationSegmentForSourceDocument> curationSegmentByBegin)
    {
        this.curationSegmentByBegin = curationSegmentByBegin;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public void setSourceDocument(SourceDocument aSourceDocument)
    {
        sourceDocument = aSourceDocument;
    }

    @Override
    public String toString()
    {
        return "curationSegmentByBegin" + curationSegmentByBegin.toString();

    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

}
