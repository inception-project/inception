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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Comparator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.Statement;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.core.Focusable;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementDetailPreference;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.model.StatementGroupBean;

/**
 * An {@code AbstractInfoPanel} offers functionality to create, display and edit instances of a
 * specific subclass of {@link KBObject}. Information about instances is shown in a
 * {@link StatementsPanel}.
 * 
 * @param <T>
 *            the type of {@link KBObject} this {@code AbstractInfoPanel} specializes on
 *            (properties, concepts, instances, ...)
 */
public abstract class AbstractInfoPanel<T extends KBObject>
    extends Panel
{

    private static final long serialVersionUID = -1413622323011843523L;

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;

    protected IModel<T> kbObjectModel;
    protected IModel<? extends KBObject> handleModel;
    protected IModel<KnowledgeBase> kbModel;

    private BootstrapModalDialog confirmationDialog;
    private SubclassCreationDialog createSubclassDialog;

    public AbstractInfoPanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<? extends KBObject> aHandleModel, IModel<T> aKbObjectModel)
    {
        super(aId, aHandleModel);
        kbModel = aKbModel;
        handleModel = aHandleModel;
        kbObjectModel = aKbObjectModel;

        setOutputMarkupId(true);

        // when creating a new KBObject, activate the form and obtain the AjaxRequestTarget to set
        // the focus to the name field
        Component content;
        createSubclassDialog = new SubclassCreationDialog("createSubclass", kbModel, handleModel);
        add(createSubclassDialog);

        boolean isNew = kbObjectModel.getObject() != null
                && isEmpty(kbObjectModel.getObject().getIdentifier());
        if (isNew) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID,
                    CompoundPropertyModel.of(kbObjectModel));

            // obtain AjaxRequestTarget and set the focus
            RequestCycle.get().find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.focusComponent(editMode.getFocusComponent()));
            content = editMode;
        }
        else {
            content = new ViewMode(CONTENT_MARKUP_ID, CompoundPropertyModel.of(handleModel),
                    getDetailPreference());
        }
        add(content);

        confirmationDialog = new BootstrapModalDialog("confirmationDialog");
        confirmationDialog.trapFocus();
        add(confirmationDialog);
    }

    protected class EditMode
        extends Fragment
        implements Focusable
    {

        private static final long serialVersionUID = -4136771477011400690L;

        private Component focusComponent;

        public EditMode(String id, CompoundPropertyModel<T> compoundModel)
        {
            super(id, "editMode", AbstractInfoPanel.this);

            // set up form components
            TextField<String> name = new TextField<>("name", compoundModel.bind("name"));
            name.add(AttributeModifier.append("placeholder",
                    new ResourceModel(getNamePlaceholderResourceKey())));
            name.setOutputMarkupId(true);
            focusComponent = name;

            // there exists functionality to cancel the "new KBObject" prompt, but in hindsight, MB
            // thinks this functionality is not really needed in the UI, so the button is hidden
            // here
            LambdaAjaxLink cancelButton = new LambdaAjaxLink("cancel",
                    AbstractInfoPanel.this::actionCancel)
                            .onConfigure((_this) -> _this.setVisible(false));
            cancelButton.add(new Label("label", new ResourceModel(getCancelButtonResourceKey())));

            LambdaAjaxButton<T> createButton = new LambdaAjaxButton<>("create",
                    AbstractInfoPanel.this::actionCreate);
            createButton.add(new Label("label", new ResourceModel(getCreateButtonResourceKey())));

            Form<T> form = new Form<T>("form", compoundModel);
            form.add(name);
            form.add(cancelButton);
            form.add(createButton);
            form.setDefaultButton(createButton);
            add(form);
        }

        @Override
        public Component getFocusComponent()
        {
            return focusComponent;
        }
    }

    protected class ViewMode
        extends Fragment
    {

        private static final long serialVersionUID = -7974486904999393082L;

        public ViewMode(String id, CompoundPropertyModel<? extends KBObject> compoundModel,
                StatementDetailPreference aDetailPreference)
        {
            super(id, "viewMode", AbstractInfoPanel.this);
            Label uiLabel = new Label("uiLabel", compoundModel.bind("uiLabel"));
            add(uiLabel);
            add(new Label("typeLabel", new ResourceModel(getTypeLabelResourceKey())));
            Label identifier = new Label("idtext");
            TooltipBehavior tip = new TooltipBehavior();
            tip.setOption("autoHide", false);
            tip.setOption("content",
                    Options.asString((compoundModel.bind("identifier").getObject())));
            tip.setOption("showOn", Options.asString("click"));
            identifier.add(tip);
            add(identifier);

            // button for deleting the KBObject
            LambdaAjaxLink deleteButton = new LambdaAjaxLink("delete",
                    AbstractInfoPanel.this::confirmActionDelete).onConfigure(
                            (_this) -> _this.setVisible(kbObjectModel.getObject() != null
                                    && isNotEmpty(kbObjectModel.getObject().getIdentifier())));
            deleteButton.add(new Label("label", new ResourceModel(getDeleteButtonResourceKey())));
            deleteButton.add(new WriteProtectionBehavior(kbModel));
            add(deleteButton);

            // button for creating a new subclass that is only visible for concepts
            LambdaAjaxLink createSubclassButton = new LambdaAjaxLink("createSubclass",
                    AbstractInfoPanel.this::actionCreateSubclass).onConfigure(
                            (_this) -> _this.setVisible(kbObjectModel.getObject() != null
                                    && isNotEmpty(kbObjectModel.getObject().getIdentifier())
                                    && kbObjectModel.getObject() instanceof KBConcept));
            createSubclassButton.add(new Label("subclassLabel",
                    new ResourceModel(getCreateSubclassButtonResourceKey())));
            createSubclassButton.add(new WriteProtectionBehavior(kbModel));
            add(createSubclassButton);

            // show statements about this KBObject
            StatementsPanel statementsPanel = new StatementsPanel("statements", kbModel,
                    handleModel, getDetailPreference());
            Comparator<StatementGroupBean> comparator = getStatementGroupComparator();
            if (comparator != null) {
                statementsPanel.setStatementGroupComparator(comparator);
            }
            add(statementsPanel);
        }
    }

    protected Comparator<StatementGroupBean> getStatementGroupComparator()
    {
        return null;
    }

    public String getCreateSubclassButtonResourceKey()
    {
        return "createSubclass";
    }

    protected String getCreateButtonResourceKey()
    {
        return "create";
    }

    protected String getCancelButtonResourceKey()
    {
        return "cancel";
    }

    protected String getDeleteButtonResourceKey()
    {
        return "delete";
    }

    protected String getNamePlaceholderResourceKey()
    {
        return "placeholder";
    }

    protected abstract String getTypeLabelResourceKey();

    public abstract List<String> getLabelProperties();

    /**
     * @return the {@link StatementDetailPreference} for the included {@link StatementsPanel}. If
     *         this method returns {@code null}, the {@code StatementDetailPreference} can be
     *         defined by the user via a selector in the UI. The default implementation returns
     *         {@code null}.
     */
    protected StatementDetailPreference getDetailPreference()
    {
        return null;
    }

    protected abstract void actionCreate(AjaxRequestTarget aTarget, Form<T> aForm);

    /**
     * Shows confirmation dialog. If the user accepts, calls
     * {@link AbstractInfoPanel#actionDelete(AjaxRequestTarget)}.
     */
    private void confirmActionDelete(AjaxRequestTarget aTarget)
    {
        // find out whether there are statements that reference the object
        List<Statement> statementsWithReference = kbService
                .listStatementsWithPredicateOrObjectReference(kbModel.getObject(),
                        kbObjectModel.getObject().getIdentifier());

        var dialogContent = new DeleteKBObjectConfirmationDialogPanel(
                BootstrapModalDialog.CONTENT_ID, handleModel, Model.of(statementsWithReference));

        dialogContent.setConfirmAction(this::actionDelete);

        confirmationDialog.open(dialogContent, aTarget);
    }

    private void actionCreateSubclass(AjaxRequestTarget aTarget)
    {
        createSubclassDialog.show(aTarget);
    }

    protected abstract void actionDelete(AjaxRequestTarget aTarget);

    protected abstract void actionCancel(AjaxRequestTarget aTarget);
}
