/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class SearchOptions extends Options
{
    private static final long serialVersionUID = 3030323391922717647L;

    private boolean limitedToCurrentDocument = false;

    private AnnotationLayer groupingLayer;

    private AnnotationFeature groupingFeature;

    private long itemsPerPage;

    public boolean isLimitedToCurrentDocument()
    {
        return limitedToCurrentDocument;
    }

    public void setLimitedToCurrentDocument(boolean aLimitedToCurrentDocument)
    {
        limitedToCurrentDocument = aLimitedToCurrentDocument;
    }

    public AnnotationLayer getGroupingLayer()
    {
        return groupingLayer;
    }

    public void setGroupingLayer(AnnotationLayer aGroupingLayer)
    {
        groupingLayer = aGroupingLayer;
    }

    public AnnotationFeature getGroupingFeature()
    {
        return groupingFeature;
    }

    public void setGroupingFeature(AnnotationFeature aGroupingFeature)
    {
        groupingFeature = aGroupingFeature;
    }

    public long getItemsPerPage()
    {
        return itemsPerPage;
    }

    public void setItemsPerPage(long aItemsPerPage)
    {
        itemsPerPage = aItemsPerPage;
    }
}
