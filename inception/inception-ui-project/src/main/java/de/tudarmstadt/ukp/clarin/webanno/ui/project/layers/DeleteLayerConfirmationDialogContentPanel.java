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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.layers;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.bootstrap.dialog.ChallengeResponseDialogContentPanel_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class DeleteLayerConfirmationDialogContentPanel
    extends ChallengeResponseDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = -943392917974988048L;

    private @SpringBean AnnotationSchemaService annotationService;

    public DeleteLayerConfirmationDialogContentPanel(String aId, IModel<AnnotationLayer> aIModel)
    {
        super(aId, new ResourceModel("title"));
        setDefaultModel(aIModel);
        setExpectedResponseModel(getModel().map(AnnotationLayer::getName));

        var blockers = LoadableDetachableModel.of(this::getDeletionBlockers);

        queue(new Label("message", new ResourceModel("message"))
                .add(visibleWhen(blockers.map(List::isEmpty))));
        queue(new Label("challenge", new ResourceModel("challenge"))
                .add(visibleWhen(blockers.map(List::isEmpty))));

        var blockersList = new ListView<String>("blockers", blockers)
        {
            private static final long serialVersionUID = -5477856044773755778L;

            @Override
            protected void populateItem(ListItem<String> aItem)
            {
                aItem.queue(new Label("blocker", aItem.getModelObject()));
            }
        };
        blockersList.add(visibleWhenNot(blockers.map(List::isEmpty)));
        queue(blockersList);

        getConfirmButton().add(visibleWhen(blockers.map(List::isEmpty)));
        getResponseField().add(visibleWhen(blockers.map(List::isEmpty)));
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotationLayer> getModel()
    {
        return (IModel<AnnotationLayer>) getDefaultModel();
    }

    private List<String> getDeletionBlockers()
    {
        var blockers = new ArrayList<String>();

        for (var relLayer : annotationService.listAttachedRelationLayers(getModel().getObject())) {
            blockers.add("Relation layer: " + relLayer.getUiName());
        }

        for (var linkFeature : annotationService.listAttachedLinkFeatures(getModel().getObject())) {
            blockers.add("Link feature: " + linkFeature.getUiName() + " (on span layer "
                    + linkFeature.getLayer().getUiName() + ")");
        }

        return blockers;
    }
}
