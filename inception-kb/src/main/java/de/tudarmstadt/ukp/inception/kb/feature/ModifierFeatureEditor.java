/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.kb.feature;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
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
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ModifierFeatureEditor
    extends FeatureEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(ModifierFeatureEditor.class);

    private static final long serialVersionUID = 7469241620229001983L;

    private @SpringBean AnnotationSchemaService annotationService;

    private @SpringBean KnowledgeBaseService kbService;

    private WebMarkupContainer content;

    // // For showing the status of Constraints rules kicking in.
    // private RulesIndicator indicator = new RulesIndicator();

    @SuppressWarnings("rawtypes")
    private Component field;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    // Wicket component is bound to this property
    @SuppressWarnings("unused")
    private KBHandle selectedRole;

    @SuppressWarnings("unchecked")
    public ModifierFeatureEditor(String aId, MarkupContainer aOwner,
                                 AnnotationActionHandler aHandler,
                                 final IModel<AnnotatorState> aStateModel,
                                 final IModel<FeatureState>
                                 aFeatureStateModel)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;

        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstraintFeature = getModelObject().feature.isHideUnconstraintFeature();

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
                    (List<LinkWithRoleModel>) ModifierFeatureEditor.this.getModelObject().value)
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

        content.add(field = createSelectRoleDropdownChoice());

        // Shows whether constraints are triggered or not
        // also shows state of constraints use.
        Component constraintsInUseIndicator = new WebMarkupContainer("linkIndicator")
        {
            private static final long serialVersionUID = 4346767114287766710L;

            @Override
            public boolean isVisible()
            {
                return getModelObject().indicator.isAffected();
            }
        }.add(new AttributeAppender("class", new Model<String>()
        {
            private static final long serialVersionUID = -7683195283137223296L;

            @Override
            public String getObject()
            {
                // adds symbol to indicator
                return getModelObject().indicator.getStatusSymbol();
            }
        })).add(new AttributeAppender("style", new Model<String>()
        {
            private static final long serialVersionUID = -5255873539738210137L;

            @Override
            public String getObject()
            {
                // adds color to indicator
                return "; color: " + getModelObject().indicator.getStatusColor();
            }
        }));
        add(constraintsInUseIndicator);

        // Add a new empty slot with the specified role
        content.add(new AjaxButton("add")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();
                setVisible(!(state.isSlotArmed() && ModifierFeatureEditor.this.getModelObject()
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
                AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && ModifierFeatureEditor.this.getModelObject()
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
                AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && ModifierFeatureEditor.this.getModelObject()
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
        AnnotationLayer linkedLayer = annotationService.getLayer(linkedType, this.stateModel
            .getObject().getProject());
        AnnotationFeature linkedAnnotationFeature = annotationService.getFeature("KBItems",
            linkedLayer);
        DropDownList<KBHandle> field = new DropDownList<KBHandle>("value",
            LambdaModelAdapter.of(
                () -> this.getSelectedKBItem(aItem, linkedAnnotationFeature),
                (v) -> { this.setSelectedKBItem(v, aItem, linkedAnnotationFeature); }
            ),
            LambdaModel.of(this::getKBConceptsAndInstances), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private void removeAutomaticallyAddedUnusedEntries()
    {
        // Remove unused (but auto-added) tags.
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> list = (List<LinkWithRoleModel>) ModifierFeatureEditor.this
            .getModelObject().value;

        // remove it
        list.removeIf(link -> link.autoCreated && link.targetAddr == -1);
    }

    private void autoAddImportantTags(List<Tag> aTagset, List<PossibleValue> aPossibleValues)
    {
        if (aTagset == null || aTagset.isEmpty() || aPossibleValues == null
            || aPossibleValues.isEmpty()) {
            return;
        }

        // Construct a quick index for tags
        Set<String> tagset = new HashSet<>();
        for (Tag t : aTagset) {
            tagset.add(t.getName());
        }

        // Get links list and build role index
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getModelObject().value;
        Set<String> roles = new HashSet<>();
        for (LinkWithRoleModel l : links) {
            roles.add(l.role);
        }

        // Loop over values to see which of the tags are important and add them.
        for (PossibleValue value : aPossibleValues) {
            if (!value.isImportant() || !tagset.contains(value.getValue())) {
                continue;
            }

            // Check if there is already a slot with the given name
            if (roles.contains(value.getValue())) {
                continue;
            }

            // Add empty slot in UI with that name.
            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = value.getValue();
            // Marking so that can be ignored later.
            m.autoCreated = true;
            links.add(m);
            // NOT arming the slot here!
        }
    }

    @Override
    public Component getFocusComponent()
    {
        return field;
    }

    /**
     * Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied and
     * feature doesn't match any constraint rule
     */
    @Override
    public void onConfigure()
    {
        // Update entries for important tags.
        removeAutomaticallyAddedUnusedEntries();
        FeatureState featureState = getModelObject();
        autoAddImportantTags(featureState.tagset, featureState.possibleValues);

        // if enabled and constraints rule execution returns anything other than green
        setVisible(!hideUnconstraintFeature || (getModelObject().indicator.isAffected()
            && getModelObject().indicator.getStatusColor().equals("green")));
    }

    private void actionAdd(AjaxRequestTarget aTarget)
    {
        if (selectedRole == null) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else {
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) ModifierFeatureEditor.this
                .getModelObject().value;
            AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();

            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = selectedRole.getUiLabel();
            links.add(m);
            state.setArmedSlot(ModifierFeatureEditor.this.getModelObject().feature,
                links.size() - 1);

            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) ModifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();

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
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) ModifierFeatureEditor.this
            .getModelObject().value;
        AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();

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
        AnnotatorState state = ModifierFeatureEditor.this.stateModel.getObject();

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
            getPredicatesFromKB(), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    private List<KBHandle> getPredicatesFromKB() {
        AnnotationFeature feat = getModelObject().feature;
        List<KBHandle> handles = new LinkedList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
            handles.addAll(kbService.listProperties(kb, false));
        }
        return new ArrayList<>(handles);
    }

    private void setSelectedKBItem(KBHandle value, Item<LinkWithRoleModel> aItem,
                                   AnnotationFeature linkedAnnotationFeature) {
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas,
                    aItem.getModelObject().targetAddr);
                WebAnnoCasUtil.setFeature(selectedFS, linkedAnnotationFeature,
                    value.getIdentifier());
            } catch (CASException | IOException e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
    }

    private KBHandle getSelectedKBItem(Item<LinkWithRoleModel> aItem, AnnotationFeature
        linkedAnnotationFeature) {
        KBHandle selectedKBHandleItem = null;
        if (aItem.getModelObject().targetAddr != -1) {
            try {
                JCas jCas = actionHandler.getEditorCas().getCas().getJCas();
                AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(jCas, aItem.getModelObject
                    ().targetAddr);
                Feature labelFeature = selectedFS.getType().getFeatureByBaseName
                    (linkedAnnotationFeature.getName());
                String selectedKBItemIdentifier = selectedFS.getFeatureValueAsString(labelFeature);
                if (selectedKBItemIdentifier != null) {
                    List<KBHandle> handles = getKBConceptsAndInstances();
                    selectedKBHandleItem = handles.stream().filter(x -> selectedKBItemIdentifier
                        .equals(x.getIdentifier())).findAny().orElse(null);
                }
            } catch (CASException | IOException e) {
                LOG.error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
        return selectedKBHandleItem;
    }

    private List<KBHandle> getKBConceptsAndInstances() {
        AnnotationFeature feat = getModelObject().feature;
        List<KBHandle> handles = new LinkedList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
            handles.addAll(kbService.listConcepts(kb, false));
            for (KBHandle concept: kbService.listConcepts(kb, false)) {
                handles.addAll(kbService.listInstances(kb, concept.getIdentifier(),
                    false));
            }
        }
        return new ArrayList<>(handles);
    }
}
