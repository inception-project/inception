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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.logout;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class LogoutTimerJavascriptResourceReference
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final LogoutTimerJavascriptResourceReference INSTANCE = new LogoutTimerJavascriptResourceReference();

    public static LogoutTimerJavascriptResourceReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private LogoutTimerJavascriptResourceReference()
    {
        super(LogoutTimerJavascriptResourceReference.class, "LogoutTimer.js");
    }

    @Override
    public List<HeaderItem> getDependencies()
    {
        List<HeaderItem> dependencies = new ArrayList<>(super.getDependencies());
        dependencies.add(forReference(WicketAjaxJQueryResourceReference.get()));
        return dependencies;
    }
}
