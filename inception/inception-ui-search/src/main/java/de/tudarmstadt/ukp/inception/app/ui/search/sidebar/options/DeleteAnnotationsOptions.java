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

public class DeleteAnnotationsOptions
    implements Serializable
{
    private static final long serialVersionUID = -2990536765778081634L;

    private boolean deleteOnlyMatchingFeatureValues = true;
    private boolean visible = false;

    public boolean isDeleteOnlyMatchingFeatureValues()
    {
        return deleteOnlyMatchingFeatureValues;
    }

    public void setDeleteOnlyMatchingFeatureValues(boolean aDeleteOnlyMatchingFeatureValues)
    {
        deleteOnlyMatchingFeatureValues = aDeleteOnlyMatchingFeatureValues;
    }

    public void setVisible(boolean aVisible)
    {
        visible = aVisible;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void toggleVisibility()
    {
        visible = !visible;
    }
}
