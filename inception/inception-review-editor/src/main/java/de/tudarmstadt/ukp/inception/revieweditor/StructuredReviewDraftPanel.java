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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class StructuredReviewDraftPanel
    extends AnnotationPanel
{
    private static final long serialVersionUID = 5419276164971178588L;

    private static final String CID_LINKED_ANNOTATIONS_CONTAINER = "linkedAnnotationsContainer";
    private static final String CID_UNLINKED_ANNOTATIONS_CONTAINER = "unlinkedAnnotationsContainer";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";

    private final WebMarkupContainer linkedAnnotationsContainer;
    private final WebMarkupContainer unlinkedAnnotationsContainer;
    private final IModel<AnnotatorState> model;

    public StructuredReviewDraftPanel(String aId, IModel<AnnotatorState> aModel,
            CasProvider aCasProvider)
    {
        super(aId, aModel, aCasProvider);

        model = aModel;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        linkedAnnotationsContainer = new WebMarkupContainer(CID_LINKED_ANNOTATIONS_CONTAINER);
        linkedAnnotationsContainer.setOutputMarkupId(true);
        linkedAnnotationsContainer.add(createLinkedAnnotationList());
        add(linkedAnnotationsContainer);

        unlinkedAnnotationsContainer = new UnlinkedAnnotationPanel(
                CID_UNLINKED_ANNOTATIONS_CONTAINER, model, aCasProvider);
        unlinkedAnnotationsContainer.setOutputMarkupId(true);
        unlinkedAnnotationsContainer.add(visibleWhen(() -> listUnlinkedAnnotations().size() > 0));

        linkedAnnotationsContainer.add(unlinkedAnnotationsContainer);
    }

    private ListView<AnnotationListItem> createLinkedAnnotationList()
    {
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS,
                LoadableDetachableModel.of(() -> listDocumentAnnotations()))
        {
            private static final long serialVersionUID = 6885792032557021315L;

            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));
                String title = aItem.getModelObject().getLayer().getUiName();
                VID vid = new VID(aItem.getModelObject().getAddr());

                DocumentAnnotationPanel panel = new DocumentAnnotationPanel(CID_ANNOTATION_DETAILS,
                        Model.of(vid), getCasProvider(), model.getObject(), title);
                aItem.add(panel);

                aItem.setOutputMarkupId(true);
            }
        };
    }
}
