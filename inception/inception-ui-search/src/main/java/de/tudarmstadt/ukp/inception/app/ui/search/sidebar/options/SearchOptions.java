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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.search.SearchResult;

public class SearchOptions
    implements Serializable
{
    private static final long serialVersionUID = 3030323391922717647L;

    private String query;
    private SearchResult selectedResult;
    private boolean limitedToCurrentDocument = false;
    private AnnotationLayer groupingLayer;
    private AnnotationFeature groupingFeature;
    private long itemsPerPage;
    private boolean lowLevelPaging;

    public void setSelectedResult(SearchResult aSelectedResult)
    {
        selectedResult = aSelectedResult;
    }

    public SearchResult getSelectedResult()
    {
        return selectedResult;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String aQuery)
    {
        query = aQuery;
    }

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

    public boolean isLowLevelPaging()
    {
        return lowLevelPaging;
    }

    public void setLowLevelPaging(boolean aLowLevelPaging)
    {
        lowLevelPaging = aLowLevelPaging;
    }
}
