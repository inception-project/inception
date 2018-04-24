/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class QualifierFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7469241620229001983L;
    private static final Logger LOG = LoggerFactory.getLogger(QualifierFeatureEditor.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FactLinkingService factService;

    private WebMarkupContainer content;
    private Component focusComponent;
    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;
    private Project project;
    private LambdaModelAdapter<KBHandle> qualifierModel;
    private KBHandle selectedRole;

    public QualifierFeatureEditor(String aId, MarkupContainer aOwner,
                                 AnnotationActionHandler aHandler,
                                 final IModel<AnnotatorState> aStateModel,
                                 final IModel<FeatureState>
                                 aFeatureStateModel)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;
        project = stateModel.getObject().getProject();

        add(new Label("feature", getModelObject().feature.getUiName()));

        // Most of the content is inside this container such that we can refresh it independently
        // from the rest of the form
        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        content.add(new RefreshingView<LinkWithRoleModel>("slots",
            PropertyModel.of(getModel(), "value"))
        {
            private static final long serialVersionUID = 5475284956525780698L;

            @Override
            protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
            {
                return new ModelIteratorAdapter<LinkWithRoleModel>(
                    (List<LinkWithRoleModel>) QualifierFeatureEditor.this.getModelObject().value)
                {
                    @Override
                    protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                    {
                        return Model.of(aObject);
                    }
                };
            }

            @Override
            protected void populateItem(final Item<LinkWithRoleModel> aItem)
            {
                AnnotatorState state = stateModel.getObject();

                aItem.setModel(new CompoundPropertyModel<>(aItem.getModelObject()));
                Label role = new Label("role");
                aItem.add(role);

                final Label label;
                if (aItem.getModelObject().targetAddr == -1
                    && state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
                    label = new Label("label", "<Select to fill>");
                }
                else {
                    label = new Label("label");
                }
                label.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = 7633309278417475424L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        actionToggleArmedState(aTarget, aItem);
                    }
                });
                label.add(new AttributeAppender("style", new Model<String>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject()
                    {
                        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
                            return "; background: orange";
                        }
                        else {
                            return "";
                        }
                    }
                }));
                aItem.add(label);
                aItem.add(createMentionKBLinkDropDown(aItem));
            }
        });

        content.add(focusComponent = createSelectRoleDropdownChoice());

        // Add a new empty slot with the specified role
        content.add(new AjaxButton("add")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(!(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature())));
                // setEnabled(!(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature())));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionAdd(aTarget);
            }
        });

        // Allows user to update slot
        content.add(new AjaxButton("set")
        {

            private static final long serialVersionUID = 7923695373085126646L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
                // setEnabled(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionSet(aTarget);
            }
        });

        // Add a new empty slot with the specified role
        content.add(new AjaxButton("del")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && QualifierFeatureEditor.this.getModelObject()
                    .feature.equals(state.getArmedFeature()));
                // setEnabled(model.isSlotArmed()
                // && aModel.feature.equals(model.getArmedFeature()));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                actionDel(aTarget);
            }
        });
    }

    private DropDownList<KBHandle> createMentionKBLinkDropDown(Item<LinkWithRoleModel> aItem)
    {
        String linkedType = this.getModelObject().feature.getType();
        AnnotationLayer linkedLayer = annotationService
            .getLayer(linkedType, this.stateModel.getObject().getProject());
        AnnotationFeature linkedAnnotationFeature = annotationService
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        qualifierModel = new LambdaModelAdapter<>(() -> this.getSelectedKBItem(aItem),  (v) -> {
            this.setSelectedKBItem((KBHandle) v, aItem, linkedAnnotationFeature);
        });
        DropDownList<KBHandle> f = new DropDownList<KBHandle>("value", qualifierModel
            , LambdaModel.of(() -> factService.getKBConceptsAndInstances(project)),
            new ChoiceRenderer<>("uiLabel"));
        f.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        f.setOutputMarkupId(true);
        return f;
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }

    @Override
    public void onConfigure()
    {

    }

    private void actionAdd(AjaxRequestTarget aTarget)
    {
        if (selectedRole == null) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
                .getModelObject().value;
            AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = selectedRole.getUiLabel();
            links.add(m);
            state.setArmedSlot(QualifierFeatureEditor.this.getModelObject().feature,
                links.size() - 1);

            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        // Update the slot
        LinkWithRoleModel m = links.get(state.getArmedSlot());
        m.role = selectedRole.getUiLabel();
        links.set(state.getArmedSlot(), m); // avoid reordering

        aTarget.add(content);

        // Commit change - but only if we set the label on a slot which was already filled/saved.
        // Unset slots only exist in the link editor and if we commit the change here, we trigger
        // a reload of the feature editors from the CAS which makes the unfilled slots disappear
        // and leaves behind an armed slot pointing to a removed slot.
        if (m.targetAddr != -1) {
            try {
                actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private void actionDel(AjaxRequestTarget aTarget)
    {
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) QualifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        links.remove(state.getArmedSlot());
        state.clearArmedSlot();

        aTarget.add(content);

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget, Item<LinkWithRoleModel> aItem)
    {
        AnnotatorState state = QualifierFeatureEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
            state.clearArmedSlot();
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject().feature, aItem.getIndex());
            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    public static void handleException(Component aComponent, AjaxRequestTarget aTarget,
                                       Exception aException)
    {
        try {
            throw aException;
        }
        catch (AnnotationException e) {
            if (aTarget != null) {
                aTarget.prependJavaScript("alert('Error: " + e.getMessage() + "')");
            }
            else {
                aComponent.error("Error: " + e.getMessage());
            }
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
        }
    }

    private DropDownChoice<KBHandle> createSelectRoleDropdownChoice()
    {
        DropDownChoice<KBHandle> field = new DropDownChoice<KBHandle>("newRole",
            new PropertyModel<KBHandle>(this, "selectedRole"),
            factService.getAllPredicatesFromKB(project), new ChoiceRenderer<>
            ("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private void setSelectedKBItem(KBHandle value, Item<LinkWithRoleModel> aItem,
                                   AnnotationFeature linkedAnnotationFeature) {
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas,
                    aItem.getModelObject().targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value.getIdentifier());
                LOG.info("change the value");
                qualifierModel.detach();

                // Save the CAS. This must be done explicitly here since the KBItem dropdown
                // is not the focus-component of this editor. In fact, there could be multiple
                // KBItem dropdowns in this feature editor since we can have multilpe modifiers.
                // For focus-components, the AnnotationFeatureForm already handles adding the
                // saving behavior.
                actionHandler.actionCreateOrUpdate(RequestCycle.get().find(AjaxRequestTarget.class),
                        jCas);
            } catch (Exception e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
    }

    private KBHandle getSelectedKBItem(Item<LinkWithRoleModel> aItem) {
        KBHandle selectedKBHandleItem = null;
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                int targetAddr = aItem.getModelObject().targetAddr;
                selectedKBHandleItem = factService.getKBHandleFromCasByAddr(jCas, targetAddr,
                    project);
            } catch (CASException | IOException e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
        return selectedKBHandleItem;
    }
}
