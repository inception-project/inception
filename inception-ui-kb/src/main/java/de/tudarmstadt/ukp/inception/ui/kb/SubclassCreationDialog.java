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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class SubclassCreationDialog
    extends ModalWindow
{
    private static final long serialVersionUID = -1304315052590065776L;
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBasePanel.class);

    public SubclassCreationDialog(String id, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> aParentConceptHandleModel)
    {
        super(id);

        setOutputMarkupPlaceholderTag(true);

        setInitialWidth(300);
        setInitialHeight(70);
        setResizable(false);
        setWidthUnit("px");
        setHeightUnit("px");
        setTitle(new Model<String>("Create Subclass"));
        setCssClassName("w_blue w_flex");
        setContent(new ContentPanel(getContentId(), aKbModel, aParentConceptHandleModel));

    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private @SpringBean KnowledgeBaseService kbService;

        CompoundPropertyModel<KBConcept> newSubclassConceptModel;
        IModel<KnowledgeBase> kbModel;
        IModel<KBHandle> parentConceptHandleModel;

        public ContentPanel(String aId, IModel<KnowledgeBase> aKbModel,
                IModel<KBHandle> aParentConceptHandleModel)
        {
            super(aId);
            kbModel = aKbModel;
            parentConceptHandleModel = aParentConceptHandleModel;
            // create property model for the subclass that is going to be created
            newSubclassConceptModel = new CompoundPropertyModel<>(
                    CompoundPropertyModel.of(Model.of((new KBConcept()))));
            newSubclassConceptModel.getObject()
                .setLanguage(kbModel.getObject().getDefaultLanguage());

            // add components for input form
            RequiredTextField<String> name = new RequiredTextField<String>("subClassName",
                    newSubclassConceptModel.bind("name"));
            name.add(AttributeModifier.append("placeholder",
                    new ResourceModel("subclassNamePlaceholder")));

            LambdaAjaxButton<KBConcept> createButton = new LambdaAjaxButton<KBConcept>(
                    "createSubclass", ContentPanel.this::actionCreateSubclass);
            createButton.add(new Label("createLabel", new ResourceModel("create")));

            LambdaAjaxLink cancelButton = new LambdaAjaxLink("cancel",
                    ContentPanel.this::actionCancel);
            cancelButton.add(new Label("cancelLabel", new ResourceModel("cancel")));

            // initialize input form and add it to the content panel
            Form<KBConcept> form = new Form<KBConcept>("form", newSubclassConceptModel);
            form.add(name);
            form.add(createButton);
            form.add(cancelButton);
            form.setDefaultButton(createButton);
            add(form);

        }

        protected void actionCreateSubclass(AjaxRequestTarget aTarget, Form<KBConcept> aForm)
        {
            try {

                // get subclassof property
                KnowledgeBase kb = kbModel.getObject();
                KBProperty property = kbService.readProperty(kb, kb.getSubclassIri().stringValue())
                        .get();
                KBHandle propertyHandle = new KBHandle(property.getIdentifier(), property.getName(),
                        property.getDescription());

                // check whether the subclass name already exists for this superclass
                List<KBHandle> existingSubclasses = kbService.listChildConcepts(kb,
                        parentConceptHandleModel.getObject().getIdentifier(), true);
                
                for (KBHandle subclass : existingSubclasses) {
                    if (newSubclassConceptModel.getObject().getName().equals(subclass.getName())) {

                        error(new StringResourceModel("createSubclassErrorMsg", this).setParameters(
                                subclass.getName(),
                                parentConceptHandleModel.getObject().getUiLabel()).getString());
                        aTarget.addChildren(getPage(), IFeedback.class);
                        return;
                    }
                }
                
                // create the new concept
                KBHandle newConceptHandle = kbService.createConcept(kb,
                        newSubclassConceptModel.getObject());

                String parentConceptId = parentConceptHandleModel.getObject().getIdentifier();

                // create the subclassof statement and add it to the knowledge base
                ValueFactory vf = SimpleValueFactory.getInstance();
                KBStatement subclassOfStmt = new KBStatement(newConceptHandle, propertyHandle,
                        vf.createIRI(parentConceptId));
                //set reification to NONE just for "upserting" the statement, then restore old value
                Reification kbReification = kb.getReification();
                kb.setReification(Reification.NONE);
                kbService.upsertStatement(kb, subclassOfStmt);
                kb.setReification(kbReification);
                
                // select newly created concept right away to show the statements
                send(getPage(), Broadcast.BREADTH,
                        new AjaxConceptSelectionEvent(aTarget, newConceptHandle,true));

            }
            catch (QueryEvaluationException | NoSuchElementException e) {
                error("Unable to find property subclassof: " + e.getLocalizedMessage());
                LOG.error("Unable to find property subclassof.", e);
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            // close the dialog
            findParent(SubclassCreationDialog.class).close(aTarget);

        }

        protected void actionCancel(AjaxRequestTarget aTarget)
        {
            // close the dialog
            findParent(SubclassCreationDialog.class).close(aTarget);

        }
    }

}
