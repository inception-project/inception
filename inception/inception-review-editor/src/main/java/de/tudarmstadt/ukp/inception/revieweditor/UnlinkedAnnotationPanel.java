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
package de.tudarmstadt.ukp.inception.revieweditor;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.revieweditor.event.RefreshEvent;

public class UnlinkedAnnotationPanel
    extends AnnotationPanel
{
    private static final long serialVersionUID = -6975253945462000226L;

    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";

    private final WebMarkupContainer annotationsContainer;
    private final IModel<AnnotatorState> model;

    public UnlinkedAnnotationPanel(String aId, IModel<AnnotatorState> aModel,
            CasProvider aCasProvider)
    {
        super(aId, aModel, aCasProvider);

        model = aModel;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        annotationsContainer = new WebMarkupContainer((CID_ANNOTATIONS_CONTAINER));
        annotationsContainer.setOutputMarkupId(true);
        annotationsContainer.add(createAnnotationList());
        add(annotationsContainer);
    }

    private ListView<AnnotationListItem> createAnnotationList()
    {
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS,
                LoadableDetachableModel.of(() -> listUnlinkedAnnotations()))
        {
            private static final long serialVersionUID = 6885792032557021315L;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                VID vid = new VID(aItem.getModelObject().getAddr());

                SpanAnnotationPanel panel = new SpanAnnotationPanel(CID_ANNOTATION_DETAILS,
                        Model.of(vid), getCas(), model.getObject());
                panel.setOutputMarkupId(true);
                aItem.add(panel);

                aItem.setOutputMarkupId(true);
            }
        };
    }

    @OnEvent
    public void onRefreshEvent(RefreshEvent event)
    {
        event.getTarget().add(this);
    }
}
