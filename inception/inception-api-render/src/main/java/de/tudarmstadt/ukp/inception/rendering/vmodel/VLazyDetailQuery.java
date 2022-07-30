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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import java.io.Serializable;

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
    implements Serializable
{
    private static final long serialVersionUID = -3223115878613737370L;

    public static final VLazyDetailQuery LAYER_LEVEL_QUERY = new VLazyDetailQuery(null, null);

    private final String feature;
    private final String query;

    public VLazyDetailQuery(String aFeature, String aQuery)
    {
        feature = aFeature;
        query = aQuery;
    }

    public String getFeature()
    {
        return feature;
    }

    public String getQuery()
    {
        return query;
    }
}
