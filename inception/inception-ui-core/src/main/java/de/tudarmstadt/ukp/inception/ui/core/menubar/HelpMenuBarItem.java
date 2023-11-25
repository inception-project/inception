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
package de.tudarmstadt.ukp.inception.ui.core.menubar;

import static de.tudarmstadt.ukp.inception.support.help.DocLink.Book.USER_GUIDE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.MissingResourceException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;

public class HelpMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 7486091139970717604L;

    private static final String CID_HELP_LINK = "helpLink";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    public HelpMenuBarItem(String aId)
    {
        super(aId);

        var helpLink = new DocLink(CID_HELP_LINK, USER_GUIDE,
                new ResourceModel("page.help.link", ""));
        helpLink.setBody(Model.of("<i class=\"fas fa-question-circle\"></i>"
                + " <span class=\"nav-link active p-0 d-none d-lg-inline\">Help</span>"));
        helpLink.add(visibleWhen(this::isPageHelpAvailable));
        add(helpLink);
    }

    private boolean isPageHelpAvailable()
    {
        try {
            // Trying to access the resources - if we can, then we show the link, but if
            // we fail, then we hide the link
            getString("page.help.link");
            return true;
        }
        catch (MissingResourceException e) {
            return false;
        }
    }
}
