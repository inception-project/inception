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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.closesession;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.config.AnnotationUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.menubar.MenuBar;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationUIAutoConfiguration#closeSessionActionBarExtension}.
 * </p>
 */
@Order(ActionBarExtension.ORDER_CLOSE_SESSION)
public class CloseSessionActionBarExtension
    implements ActionBarExtension
{
    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        return aPage.visitChildren(MenuBar.class, (c, v) -> {
            v.stop(!((MenuBar) c).isVisible());
        });
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new CloseSessionPanel(aId, aPage);
    }
}
