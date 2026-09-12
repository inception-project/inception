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

import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface ActionBarExtension
    extends Extension<ActionBarContext>
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

    public static final int PRIORITY_GLOBAL = 1000;
    public static final int PRIORITY_LOCAL = 2000;

    @Override
    default String getId()
    {
        return getClass().getName();
    }

    @Override
    default boolean accepts(ActionBarContext aContext)
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

    Component createActionBarItem(String aId, ActionBarContext aContext);

    /**
     * Called when the {@link ActionBar} is added or when its contents change based on the state and
     * the {@link #accepts(ActionBarContext)} method. This allows the action bar extensions e.g. to
     * inject behaviors before their items are even visible on screen. by whichever action bar
     * initializes first.
     */
    default void onInitialize(ActionBarContext aContext)
    {
        // Do nothing by default
    }

    /**
     * Called when the {@link ActionBar} contents change based on the state and the
     * {@link #accepts(ActionBarContext)} method.
     */
    default void onRemove(ActionBarContext aContext)
    {
        // Do nothing by default
    }
}
