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
package de.tudarmstadt.ukp.inception.feature.lookup;

import java.util.ArrayList;
import java.util.List;

import org.wicketstuff.jquery.core.template.IJQueryTemplate;

final class LookupEntryTemplate
    implements IJQueryTemplate
{
    private static final long serialVersionUID = 8656996525796349138L;

    @Override
    public String getText()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<span class='item-title'>${ data.uiLabel }</span>");
        sb.append("<div class='item-description'>${ data.description }</div>");
        return sb.toString();
    }

    @Override
    public List<String> getTextProperties()
    {
        List<String> properties = new ArrayList<>();
        properties.add("identifier");
        properties.add("description");
        return properties;
    }
}
