/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A model for curation container comprises of {@link CurationViewForSourceDocument}, {@link SourceDocument},
 * and {@link Project}
 * @author  Andreas Straninger
 * @author  Seid Muhie Yimam
 *
 */
public class CurationContainer
    implements Serializable
{
    private static final long serialVersionUID = -6632707037285383353L;

    private Map<Integer, CurationViewForSourceDocument> curationViewByBegin = new HashMap<Integer, CurationViewForSourceDocument>();

    private BratAnnotatorModel bratAnnotatorModel;

    public List<CurationViewForSourceDocument> getCurationViews()
    {
        LinkedList<Integer> viewsBegin = new LinkedList<Integer>(curationViewByBegin.keySet());
        Collections.sort(viewsBegin);
        List<CurationViewForSourceDocument> curationViews = new LinkedList<CurationViewForSourceDocument>();
        for (Integer begin : viewsBegin) {
            curationViews.add(curationViewByBegin.get(begin));
        }
        return curationViews;
    }

    public Map<Integer, CurationViewForSourceDocument> getCurationViewByBegin()
    {
        return curationViewByBegin;
    }

    public void setCurationSegmentByBegin(Map<Integer, CurationViewForSourceDocument> curationViewByBegin)
    {
        this.curationViewByBegin = curationViewByBegin;
    }


    @Override
    public String toString()
    {
        return "curationSegmentByBegin" + curationViewByBegin.toString();

    }

    public BratAnnotatorModel getBratAnnotatorModel()
    {
        return bratAnnotatorModel;
    }

    public void setBratAnnotatorModel(BratAnnotatorModel bratAnnotatorModel)
    {
        this.bratAnnotatorModel = bratAnnotatorModel;
    }

}
