/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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


package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.EmbeddableImage;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.page.MonitoringPage;

public class MetaImageColumn extends AbstractColumn
{

    private static final ResourceReference ICON_FINISHED = new PackageResourceReference(
        MonitoringPage.class, "resultset_next.png");


    public MetaImageColumn(IModel displayModel, Object sortProperty)
    {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(Item aItem, String aID, IModel aModel)
    {

        //Add the Icon
        EmbeddableImage icon = new EmbeddableImage(aID, ICON_FINISHED);

        icon.add(new AttributeAppender("style", "cursor: pointer", ";"));
        aItem.add(icon);
        aItem.add(AttributeModifier.append("class", "centering"));

        //Create the Panel on click event
        final ModalWindow modalPanel;
        aItem.add(modalPanel = new ModalWindow("modalPanel"));
        ModalPanel panel = new ModalPanel(modalPanel.getContentId());
        modalPanel.setContent(panel);
        modalPanel.setTitle("Metadata");
        aItem.add(modalPanel);
        aItem.add(new AjaxEventBehavior("click")

        {
            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                System.out.println("Clicked");
                modalPanel.show(aTarget);
            }
        });
    }


    private class ModalPanel extends Panel
    {
        public ModalPanel(String id)
        {
            super(id);

            add(new Label("test"));
        }

    }
}
