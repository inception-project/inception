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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.wicketstuff.kendo.ui.settings.KendoUILibrarySettings;

public class KendoResourceBehavior
    extends Behavior
{
    private static final long serialVersionUID = 9065700322035556746L;

    private static final KendoResourceBehavior INSTANCE = new KendoResourceBehavior();

    public static KendoResourceBehavior get()
    {
        return INSTANCE;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        // We use Kendo TextFields, but they do not automatically load the Kendo
        // JS/CSS, so
        // we do it manually here and for all the pages.
        KendoUILibrarySettings kendoCfg = KendoUILibrarySettings.get();

        if (kendoCfg.getCommonStyleSheetReference() != null) {
            aResponse.render(CssHeaderItem.forReference(kendoCfg.getCommonStyleSheetReference()));
        }

        if (kendoCfg.getThemeStyleSheetReference() != null) {
            aResponse.render(CssHeaderItem.forReference(kendoCfg.getThemeStyleSheetReference()));
        }

        if (kendoCfg.getJavaScriptReference() != null) {
            aResponse.render(JavaScriptHeaderItem.forReference(kendoCfg.getJavaScriptReference()));
        }
    }
}
