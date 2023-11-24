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
package de.tudarmstadt.ukp.inception.support.kendo;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class WicketJQueryFocusPatch
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final WicketJQueryFocusPatch INSTANCE = new WicketJQueryFocusPatch();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static WicketJQueryFocusPatch get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private WicketJQueryFocusPatch()
    {
        super(WicketJQueryFocusPatch.class, "wicket-jquery-focus-patch.js");
    }

    @Override
    public List<HeaderItem> getDependencies()
    {
        var dependencies = new ArrayList<HeaderItem>();
        dependencies.addAll(super.getDependencies());
        dependencies
                .add(JavaScriptHeaderItem.forReference(WicketAjaxJQueryResourceReference.get()));
        return dependencies;
    }
}
