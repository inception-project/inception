/*
 * Copyright 2019
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model;

/**
 * Query for a lazy detail. 
 * <p>
 * Some information is only to be shown when the user performs a particular "detail information"
 * action, e.g. hovering the mouse over an annotation. This class represents a query which is 
 * triggered at such a time to load additional information from the server.
 * </p>
 * 
 * @see VLazyDetailResult
 */
public class VLazyDetailQuery
{
    private String feature;
    private String query;
    
    public VLazyDetailQuery(String aFeature, String aQuery)
    {
        feature = aFeature;
        query = aQuery;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String aQuery)
    {
        query = aQuery;
    }
}
