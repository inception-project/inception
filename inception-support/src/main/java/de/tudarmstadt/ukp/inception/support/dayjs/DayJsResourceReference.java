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
package de.tudarmstadt.ukp.inception.support.dayjs;

import static java.util.Arrays.stream;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.head.HeaderItem;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

public class DayJsResourceReference
    extends WebjarsJavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    public static enum DayJsPlugin
    {
        RELATIVE_TIME("relativeTime"), //
        LOCALIZED_FORMAT("localizedFormat");

        private String name;

        private DayJsPlugin(String aName)
        {
            name = aName;
        }
    }

    private Set<DayJsPlugin> plugins;

    public DayJsResourceReference(DayJsPlugin... aPlugins)
    {
        super("dayjs/current/dayjs.min.js");
        plugins = new HashSet<>();
        stream(aPlugins).forEach(plugins::add);
    }

    @Override
    public List<HeaderItem> getDependencies()
    {
        List<HeaderItem> dependencies = new ArrayList<>(super.getDependencies());

        for (DayJsPlugin plugin : plugins) {
            dependencies.add(forReference(new WebjarsJavaScriptResourceReference(
                    "dayjs/current/plugin/" + plugin.name + ".js")));
            dependencies.add(forScript("document.addEventListener('DOMContentLoaded', () => { " //
                    + "dayjs.extend(window.dayjs_plugin_" + plugin.name + ")});",
                    "dayjs_plugin_" + plugin.name));
        }

        return dependencies;
    }
}
