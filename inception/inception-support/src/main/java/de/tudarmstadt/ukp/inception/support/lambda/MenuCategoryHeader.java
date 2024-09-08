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
package de.tudarmstadt.ukp.inception.support.lambda;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.jquery.ui.JQueryIcon;
import org.wicketstuff.jquery.ui.widget.menu.AbstractMenuItem;

public class MenuCategoryHeader
    extends AbstractMenuItem
{
    private static final long serialVersionUID = 4051171004089469088L;

    public MenuCategoryHeader(String title)
    {
        this(Model.of(title), JQueryIcon.NONE);
    }

    public MenuCategoryHeader(IModel<String> aTitle, String aIcon)
    {
        super(aTitle, aIcon);
        setEnabled(false);
    }

    @Override
    final public void onClick(AjaxRequestTarget aTarget)
    {
        // Nothing to do
    }
}
