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
package de.tudarmstadt.ukp.inception.bootstrap;

import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;

/**
 * Bootstrap-compatible {@code AjaxTabbedPanel}.
 * 
 * Mildly inspired by <a href="https://gist.github.com/raphw/7824600">this Github Gist</a>.
 * 
 * @param <T>
 *            the tab type
 */
public class BootstrapAjaxTabbedPanel<T extends ITab>
    extends AjaxTabbedPanel<T>
{
    private static final long serialVersionUID = -1207096969482559390L;

    public BootstrapAjaxTabbedPanel(String id, List<T> tabs)
    {
        super(id, tabs);
    }

    public BootstrapAjaxTabbedPanel(String id, List<T> tabs, IModel<Integer> model)
    {
        super(id, tabs, model);
    }

    @Override
    protected String getSelectedTabCssClass()
    {
        return "active";
    }

    @Override
    protected String getTabContainerCssClass()
    {
        return "nav nav-tabs card-header-tabs";
    }
}
