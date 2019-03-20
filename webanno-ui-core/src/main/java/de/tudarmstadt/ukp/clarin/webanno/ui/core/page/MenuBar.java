/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import java.util.MissingResourceException;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.support.ImageLinkDecl;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ImageLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.logout.LogoutPanel;

public class MenuBar
    extends Panel
{
    private static final long serialVersionUID = -8018701379688272826L;

    private ExternalLink helpLink;
    private ListView<ImageLinkDecl> links;

    public MenuBar(String aId)
    {
        super(aId);

        add(new LogoutPanel("logoutPanel"));

        add(helpLink = new ExternalLink("helpLink", new ResourceModel("page.help.link", ""),
                new ResourceModel("page.help", "")) {
            private static final long serialVersionUID = -2510064191732926764L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                try {
                    getString("page.help.link");
                    getString("page.help");
                    helpLink.setVisible(true);
                }
                catch (MissingResourceException e) {
                    helpLink.setVisible(false);
                }
            }
        });
        helpLink.setContextRelative(true);
        helpLink.setPopupSettings(new PopupSettings("_blank"));
        
        links = new ListView<ImageLinkDecl>("links", SettingsUtil.getLinks())
        {
            private static final long serialVersionUID = 1768830545639450786L;

            @Override
            protected void populateItem(ListItem<ImageLinkDecl> aItem)
            {
                aItem.add(new ImageLink("link",
                        new UrlResourceReference(Url.parse(aItem.getModelObject().getImageUrl())),
                        Model.of(aItem.getModelObject().getLinkUrl())));
            }
        };
        add(links);
    }
}
