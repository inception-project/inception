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
package de.tudarmstadt.ukp.inception.guidelines;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.curation.settings.LegacySplitCurationPageProperties;

/**
 * Provides access to the annotation guidelines from the action bar of the
 * {@link LegacyCurationPage}.
 *
 * @deprecated Every other page offers guidelines through the sidebar instead. The legacy curation
 *             page has no sidebar to host that tab, so it keeps the action bar button until the
 *             page itself is removed - at which point this extension goes with it.
 */
@Deprecated
@Order(ActionBarExtension.ORDER_GUIDELINES)
public class GuidelinesActionBarExtension
    implements ActionBarExtension
{
    private final GuidelinesService guidelinesService;
    private final LegacySplitCurationPageProperties curationPageProperties;

    public GuidelinesActionBarExtension(GuidelinesService aGuidelinesService,
            LegacySplitCurationPageProperties aCurationPageProperties)
    {
        guidelinesService = aGuidelinesService;
        curationPageProperties = aCurationPageProperties;
    }

    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        if (!curationPageProperties.isEnabled()) {
            return false;
        }

        if (!(aContext.page() instanceof LegacyCurationPage)) {
            return false;
        }

        // Hide the guidelines item if there are no guidelines
        return ActionBarExtension.super.accepts(aContext)
                && aContext.page().getModelObject().getProject() != null
                && guidelinesService.hasGuidelines(aContext.page().getModelObject().getProject());
    }

    @Override
    public Panel createActionBarItem(String aId, ActionBarContext aContext)
    {
        return new GuidelinesActionBarItem(aId, aContext.page());
    }
}
