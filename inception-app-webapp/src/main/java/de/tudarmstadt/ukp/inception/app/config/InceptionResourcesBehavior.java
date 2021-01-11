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
package de.tudarmstadt.ukp.inception.app.config;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoJavascriptReference;
import de.tudarmstadt.ukp.inception.app.css.InceptionCssReference;

public class InceptionResourcesBehavior
    extends Behavior
{
    private static final long serialVersionUID = 5367089196863803403L;

    private static final InceptionResourcesBehavior INSTANCE = new InceptionResourcesBehavior();

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        // Loading WebAnno CSS here so it can override JQuery/Kendo CSS
        aResponse.render(CssHeaderItem.forReference(InceptionCssReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebAnnoJavascriptReference.get()));
    }

    public static InceptionResourcesBehavior get()
    {
        return INSTANCE;
    }
}
