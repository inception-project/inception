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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import org.apache.wicket.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface ActionBarExtension
    extends Extension<AnnotationPageBase>
{
    public static final int ORDER_DOCUMENT_NAVIGATOR = 0;
    public static final int ORDER_UNDO = 1000;
    public static final int ORDER_PAGING = 2000;
    public static final int ORDER_GUIDELINES = 3000;
    public static final int ORDER_SCRIPT_DIRECTION = 4000;
    public static final int ORDER_WORKFLOW = 5000;
    public static final int ORDER_RECOMMENDER = 6000;
    public static final int ORDER_SETTINGS = 7000;
    public static final int ORDER_CLOSE_SESSION = 10000;

    public static final String ROLE_NAVIGATOR = "navigator";

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

    Component createActionBarItem(String aId, AnnotationPageBase aPage);

    /**
     * Called when the {@link ActionBar} is added to the page or when its contents change based on
     * the page state and the {@link #accepts(AnnotationPageBase)} method. This allows the action
     * bar extensions e.g. to inject behaviors into the page before their items are even visible on
     * screen.
     */
    default void onInitialize(AnnotationPageBase aPage)
    {
        // Do nothing by default
    }

    /**
     * Called when the {@link ActionBar} contents change based on the page state and the
     * {@link #accepts(AnnotationPageBase)} method. This allows the action bar extensions e.g. to
     * inject behaviors into the page before their items are even visible on screen.
     */
    default void onRemove(AnnotationPageBase aPage)
    {
        // Do nothing by default
    }
}
