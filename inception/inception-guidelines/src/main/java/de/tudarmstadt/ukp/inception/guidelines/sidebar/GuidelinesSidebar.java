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
package de.tudarmstadt.ukp.inception.guidelines.sidebar;

import static java.util.Collections.emptyList;
import static org.apache.wicket.markup.html.link.PopupSettings.RESIZABLE;
import static org.apache.wicket.markup.html.link.PopupSettings.SCROLLBARS;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.guidelines.GuidelinesService;

public class GuidelinesSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3358829174917663533L;

    private @SpringBean GuidelinesService guidelinesService;

    public GuidelinesSidebar(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aAnnotationPage);

        var guidelines = LoadableDetachableModel.of(this::listGuidelines);

        add(new ListView<String>("guidelines", guidelines)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> aItem)
            {
                var name = aItem.getModelObject();

                var stream = new FileResourceStream(
                        guidelinesService.getGuideline(getProject(), name));
                var link = new ResourceLink<Void>("guideline", new ResourceStreamResource(stream));
                link.setPopupSettings(
                        new PopupSettings(RESIZABLE | SCROLLBARS).setHeight(500).setWidth(700));
                link.add(new Label("guidelineName", name));

                aItem.add(link);
            }
        });
    }

    private List<String> listGuidelines()
    {
        var project = getProject();

        if (project == null) {
            return emptyList();
        }

        return guidelinesService.listGuidelines(project);
    }

    private Project getProject()
    {
        return getModelObject().getProject();
    }
}
