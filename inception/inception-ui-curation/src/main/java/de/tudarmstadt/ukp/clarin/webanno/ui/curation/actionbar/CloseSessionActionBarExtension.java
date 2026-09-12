/*
 * Licensed to the Technische UniversitÃ¤t Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische UniversitÃ¤t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.config.LegacyCurationUIAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.ui.core.menubar.MenuBar;

/**
 * Offers a way out of a session from the action bar of the {@link LegacyCurationPage}.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link LegacyCurationUIAutoConfiguration#closeSessionActionBarExtension}.
 * </p>
 *
 * @deprecated Every other page offers this through the sidebar footer instead. The legacy curation
 *             page has no sidebar to host that item, so it keeps the action bar button until the
 *             page itself is removed - at which point this extension goes with it.
 */
@Deprecated
@Order(ActionBarExtension.ORDER_CLOSE_SESSION)
public class CloseSessionActionBarExtension
    implements ActionBarExtension
{
    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        if (!(aContext.page() instanceof LegacyCurationPage)) {
            return false;
        }

        return aContext.page().visitChildren(MenuBar.class, (c, v) -> {
            v.stop(!((MenuBar) c).isVisible());
        });
    }

    @Override
    public Panel createActionBarItem(String aId, ActionBarContext aContext)
    {
        return new CloseSessionPanel(aId);
    }
}
