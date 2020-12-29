/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;

public interface ActionBarExtension
    extends Extension<AnnotationPageBase>
{
    @Override
    default String getId()
    {
        return getClass().getName();
    }

    @Override
    default boolean accepts(AnnotationPageBase aPage)
    {
        return true;
    }

    /**
     * @return the role of the action bar extension.
     */
    default String getRole()
    {
        return getClass().getName();
    }

    /**
     * For a given {@link #getRole() role}, only one extension is added to the action bar. If
     * multiple extensions apply in the context, then the one with the highest priority is used.
     * 
     * @return the priority
     */
    default int getPriority()
    {
        return 0;
    }

    Panel createActionBarItem(String aId, AnnotationPageBase aPage);

    /**
     * Called when the {@link ActionBar} is added to the page. This allows the action bar extensions
     * e.g. to inject behaviors into the page before their items are even visible on screen.
     */
    default void onInitialize(AnnotationPageBase aPage)
    {
        // Do nothing by default
    }
}
