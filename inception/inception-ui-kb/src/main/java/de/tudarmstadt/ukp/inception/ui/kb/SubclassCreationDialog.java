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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;

public class SubclassCreationDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = -1304315052590065776L;

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBasePanel.class);

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KBConcept> newSubclassConceptModel;
    private IModel<KnowledgeBase> kbModel;
    private IModel<? extends KBObject> parentConceptHandleModel;

    public SubclassCreationDialog(String id, IModel<KnowledgeBase> aKbModel,
            IModel<? extends KBObject> aParentConceptHandleModel)
    {
        super(id);

        trapFocus();
        closeOnEscape();
        closeOnClick();

        kbModel = aKbModel;
        parentConceptHandleModel = aParentConceptHandleModel;
        newSubclassConceptModel = Model.of(new KBConcept());
        newSubclassConceptModel.getObject().setLanguage(kbModel.getObject().getDefaultLanguage());

        setOutputMarkupPlaceholderTag(true);
    }

    public void show(AjaxRequestTarget aTarget)
    {
        var content = new ContentPanel(ModalDialog.CONTENT_ID, newSubclassConceptModel);
        aTarget.focusComponent(content.nameField);
        open(content, aTarget);
    }

    protected void actionCreateSubclass(AjaxRequestTarget aTarget, Form<KBConcept> aForm)
    {
        try {
            // get subclassof property
            KnowledgeBase kb = kbModel.getObject();
            KBProperty property = kbService.readProperty(kb, kb.getSubclassIri()).get();

            // check whether the subclass name already exists for this superclass
            List<KBHandle> existingSubclasses = kbService.listChildConcepts(kb,
                    parentConceptHandleModel.getObject().getIdentifier(), true);

            for (KBHandle subclass : existingSubclasses) {
                if (newSubclassConceptModel.getObject().getName().equals(subclass.getName())) {

                    error(new StringResourceModel("createSubclassErrorMsg", this)
                            .setParameters(subclass.getName(),
                                    parentConceptHandleModel.getObject().getUiLabel())
                            .getString());
                    aTarget.addChildren(getPage(), IFeedback.class);
                    return;
                }
            }

            // create the new concept
            var newConcept = newSubclassConceptModel.getObject();
            kbService.createConcept(kb, newConcept);

            var parentConceptId = parentConceptHandleModel.getObject().getIdentifier();

            // create the subclassof statement and add it to the knowledge base
            ValueFactory vf = SimpleValueFactory.getInstance();
            KBStatement subclassOfStmt = new KBStatement(null, newConcept.toKBHandle(), property,
                    vf.createIRI(parentConceptId));
            // set reification to NONE just for "upserting" the statement, then restore old value
            Reification kbReification = kb.getReification();
            try {
                kb.setReification(Reification.NONE);
                kbService.upsertStatement(kb, subclassOfStmt);
            }
            finally {
                kb.setReification(kbReification);
            }

            // close the dialog - this needs to be done before sending the event below because
            // that event will cause changes to the page layout and this in turn will trigger an
            // exception if we try to close the dialog after the layout change
            close(aTarget);

            // select newly created concept right away to show the statements
            send(SubclassCreationDialog.this.getPage(), Broadcast.BREADTH,
                    new AjaxConceptSelectionEvent(aTarget, newConcept.toKBHandle(), true));
        }
        catch (Exception e) {
            error("Unable to find property subclassof: " + e.getLocalizedMessage());
            LOG.error("Unable to find property subclassof.", e);
            aTarget.addChildren(SubclassCreationDialog.this.getPage(), IFeedback.class);
        }
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private final RequiredTextField<String> nameField;

        public ContentPanel(String aId, IModel<KBConcept> newSubclassConceptModel)
        {
            super(aId);

            // add components for input form
            nameField = new RequiredTextField<>("name");
            nameField.add(AttributeModifier.append("placeholder",
                    new ResourceModel("subclassNamePlaceholder")));
            nameField.setOutputMarkupId(true);
            queue(nameField);

            LambdaAjaxButton<KBConcept> createButton = new LambdaAjaxButton<KBConcept>(
                    "createSubclass", SubclassCreationDialog.this::actionCreateSubclass);
            queue(createButton);
            queue(new Label("createLabel", new ResourceModel("create")));

            queue(new LambdaAjaxLink("cancel", this::actionCloseDialog));
            queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));

            // initialize input form and add it to the content panel
            Form<KBConcept> form = new Form<KBConcept>("form",
                    CompoundPropertyModel.of(newSubclassConceptModel));
            form.setDefaultButton(createButton);

            queue(form);
        }

        protected void actionCloseDialog(AjaxRequestTarget aTarget)
        {
            findParent(ModalDialog.class).close(aTarget);
        }
    }
}
