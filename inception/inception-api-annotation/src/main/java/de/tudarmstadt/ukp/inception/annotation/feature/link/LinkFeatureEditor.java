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
package de.tudarmstadt.ukp.inception.annotation.feature.link;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.ReorderableTagAutoCompleteField;
import de.tudarmstadt.ukp.inception.annotation.feature.string.ClassicKendoComboboxTextFeatureEditor;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderSlotsEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeUtil;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

public class LinkFeatureEditor
    extends FeatureEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(LinkFeatureEditor.class);

    private static final long serialVersionUID = 7469241620229001983L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean LinkFeatureSupportProperties properties;

    private WebMarkupContainer content;

    // // For showing the status of Constraints rules kicking in.
    // private RulesIndicator indicator = new RulesIndicator();

    @SuppressWarnings("rawtypes")
    private final AbstractTextComponent field;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    // Wicket component is bound to this property
    @SuppressWarnings("unused")
    private String newRole;

    @SuppressWarnings("unchecked")
    public LinkFeatureEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
            final IModel<AnnotatorState> aStateModel, final IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        LinkFeatureTraits traits = getTraits();

        stateModel = aStateModel;
        actionHandler = aHandler;

        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstraintFeature = getModelObject().feature.isHideUnconstraintFeature();

        // Most of the content is inside this container such that we can refresh it independently
        // from the rest of the form
        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        IModel<List<LinkWithRoleModel>> slotsModel = getModel() //
                .map(FeatureState::getValue) //
                .map(v -> (List<LinkWithRoleModel>) v);
        content.add(new RefreshingView<LinkWithRoleModel>("slots", slotsModel)
        {
            private static final long serialVersionUID = 5475284956525780698L;

            @Override
            public void onConfigure()
            {
                super.onConfigure();
                setOutputMarkupPlaceholderTag(true);
                setVisible(!slotsModel.map(List::isEmpty).orElse(true).getObject());
            }

            @Override
            protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
            {
                return new ModelIteratorAdapter<LinkWithRoleModel>(
                        (List<LinkWithRoleModel>) LinkFeatureEditor.this.getModelObject().value)
                {
                    @Override
                    protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                    {
                        return Model.of(aObject);
                    }
                };
            }

            private String getRole(LinkWithRoleModel aModel)
            {
                AnnotatorState state = stateModel.getObject();

                if (aModel.targetAddr <= -1) {
                    return aModel.role;
                }

                CAS cas;
                try {
                    cas = actionHandler.getEditorCas();
                }
                catch (IOException e) {
                    handleException(this, null, e);
                    return "";
                }

                FeatureStructure fs = ICasUtil.selectFsByAddr(cas, aModel.targetAddr);
                AnnotationLayer layer = annotationService.findLayer(state.getProject(), fs);

                if (!traits.isEnableRoleLabels()) {
                    return layer.getUiName();
                }

                TypeAdapter adapter = annotationService.getAdapter(layer);
                Renderer renderer = layerSupportRegistry.getLayerSupport(layer).createRenderer(
                        layer, () -> annotationService.listAnnotationFeature(layer));
                List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
                Map<String, String> renderedFeatures = renderer.renderLabelFeatureValues(adapter,
                        fs, features);

                String roleLabel = TypeUtil.getUiLabelText(renderedFeatures);
                if (isEmpty(roleLabel)) {
                    roleLabel = aModel.role + ": " + layer.getUiName();
                }
                else {
                    roleLabel = aModel.role + ": " + layer.getUiName() + ": " + roleLabel;
                }

                return roleLabel;
            }

            @Override
            protected void populateItem(final Item<LinkWithRoleModel> aItem)
            {
                AnnotatorState state = stateModel.getObject();

                aItem.setModel(new CompoundPropertyModel<>(aItem.getModelObject()));

                FeatureState featureState = LinkFeatureEditor.this.getModelObject();
                IModel<String> roleModel = LoadableDetachableModel
                        .of(() -> getRole(aItem.getModelObject()));
                Label labelComponent = new Label("role", roleModel);
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                if (featureState.tagset != null) {
                    String role = aItem.getModelObject().role;
                    String description = featureState.tagset.stream()
                            .filter(t -> role.equals(t.getName())).findFirst()
                            .map(ReorderableTag::getDescription).orElse("No description");
                    labelComponent.add(
                            new DescriptionTooltipBehavior(roleModel.getObject(), description));
                }
                aItem.add(labelComponent);

                aItem.add(new LambdaAjaxLink("jumpToAnnotation",
                        _target -> actionHandler.actionSelectAndJump(_target,
                                new VID(aItem.getModelObject().targetAddr))) //
                                        .setAlwaysEnabled(true) //
                                        .add(visibleWhen(
                                                () -> aItem.getModelObject().targetAddr != -1)));

                final Label label;
                if (aItem.getModelObject().targetAddr == -1
                        && state.isArmedSlot(getModelObject(), aItem.getIndex())) {
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
                        if (state.isArmedSlot(getModelObject(), aItem.getIndex())) {
                            return "; background: orange";
                        }
                        else {
                            return "";
                        }
                    }
                }));
                aItem.add(label);
            }
        });

        if (getModelObject().feature.getTagset() != null) {
            if (getModelObject().tagset.size() > properties.getAutoCompleteThreshold()) {
                field = makeAutoComplete("newRole");
            }
            else {
                field = makeComboBox("newRole");
            }
        }
        else {
            field = new TextField<String>("newRole", PropertyModel.of(this, "newRole"));
        }

        field.add(LambdaBehavior.onConfigure(_this -> {
            // If a slot is armed, then load the slot's role into the dropdown
            FeatureState featureState = LinkFeatureEditor.this.getModelObject();
            AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) featureState.value;

            if (state.isSlotArmed() && featureState.feature.equals(state.getArmedFeature().feature)
                    && links.size() > state.getArmedSlot()) {
                field.setModelObject(links.get(state.getArmedSlot()).role);
            }
            else {
                field.setModelObject("");
            }
        }));
        field.setOutputMarkupId(true);
        field.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        field.add(visibleWhen(traits::isEnableRoleLabels));
        content.add(field);

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
        content.add(new LambdaAjaxLink("add", this::actionAdd)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();
                setVisible(!(state.isSlotArmed() && LinkFeatureEditor.this.getModelObject().feature
                        .equals(state.getArmedFeature().feature)));
            }
        });

        // Allows user to update slot
        LambdaAjaxLink setBtn = new LambdaAjaxLink("set", this::actionSet);
        setBtn.add(visibleWhen(
                () -> traits.isEnableRoleLabels() && stateModel.getObject().isSlotArmed()
                        && LinkFeatureEditor.this.getModelObject().feature
                                .equals(stateModel.getObject().getArmedFeature().feature)));
        content.add(setBtn);

        // Add a new empty slot with the specified role
        content.add(new LambdaAjaxLink("del", this::actionDel)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && LinkFeatureEditor.this.getModelObject().feature
                        .equals(state.getArmedFeature().feature));
            }
        });
    }

    private AbstractTextComponent makeAutoComplete(String aId)
    {
        return new ReorderableTagAutoCompleteField(aId, PropertyModel.of(this, "newRole"),
                getModel(), properties.getAutoCompleteMaxResults());
    }

    private AbstractTextComponent makeComboBox(String aId)
    {
        return new StyledComboBox<Tag>(aId, PropertyModel.of(this, "newRole"),
                PropertyModel.of(getModel(), "tagset"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();

                // Ensure proper order of the initializing JS header items: first combo box
                // behavior (in super.onInitialize()), then tooltip.
                Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                options.set("content", ClassicKendoComboboxTextFeatureEditor.FUNCTION_FOR_TOOLTIP);
                add(new TooltipBehavior("#" + field.getMarkupId() + "_listbox *[title]", options)
                {
                    private static final long serialVersionUID = -7207021885475073279L;

                    @Override
                    protected String $()
                    {
                        // REC: It takes a moment for the KendoDatasource to load the data and
                        // for the Combobox to render the hidden dropdown. I did not find
                        // a way to hook into this process and to get notified when the
                        // data is available in the dropdown, so trying to handle this
                        // with a slight delay hoping that all is set up after 1 second.
                        return "try {setTimeout(function () { " + super.$()
                                + " }, 1000); } catch (err) {}; ";
                    }
                });
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);

                aBehavior.setOption("placeholder", Options.asString("Select role"));

                // Trigger a re-loading of the tagset from the server as constraints may have
                // changed the ordering
                Optional<AjaxRequestTarget> target = RequestCycle.get()
                        .find(AjaxRequestTarget.class);
                if (target.isPresent()) {
                    target.get()
                            .appendJavaScript(WicketUtil.wrapInTryCatch(String.format(
                                    "var $w = %s; if ($w) { $w.dataSource.read(); }",
                                    KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD))));
                }
            }
        };
    }

    private void removeAutomaticallyAddedUnusedEntries()
    {
        // Remove unused (but auto-added) tags.
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> list = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;

        // remove it
        list.removeIf(link -> link.autoCreated && link.targetAddr == -1);
    }

    private void autoAddDefaultSlots()
    {
        LinkFeatureTraits traits = getTraits();

        // Get links list and build role index
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getModelObject().value;
        Set<String> roles = new HashSet<>();
        for (LinkWithRoleModel l : links) {
            roles.add(l.role);
        }

        for (long id : traits.getDefaultSlots()) {
            Optional<Tag> optionalTag = annotationService.getTag(id);

            // If a tag is missing, ignore it. We do not have foreign-key constraints in
            // traits, so it is not an unusal situation that a user deletes a tag still
            // referenced in a trait.
            if (optionalTag.isPresent()) {
                Tag tag = optionalTag.get();

                // Check if there is already a slot with the given name
                if (roles.contains(tag.getName())) {
                    continue;
                }

                // Add empty slot in UI with that name.
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = tag.getName();
                // Marking so that can be ignored later.
                m.autoCreated = true;
                links.add(m);
                // NOT arming the slot here!
            }
        }
    }

    private void autoAddImportantTags(List<ReorderableTag> aTagset,
            List<PossibleValue> aPossibleValues)
    {
        if (aTagset == null || aTagset.isEmpty() || aPossibleValues == null
                || aPossibleValues.isEmpty()) {
            return;
        }

        // Construct a quick index for tags
        Set<String> tagset = new HashSet<>();
        for (ReorderableTag t : aTagset) {
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
    public FormComponent getFocusComponent()
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
        super.onConfigure();

        // Update entries for important tags.
        removeAutomaticallyAddedUnusedEntries();
        FeatureState featureState = getModelObject();
        autoAddDefaultSlots();
        autoAddImportantTags(featureState.tagset, featureState.possibleValues);

        // if enabled and constraints rule execution returns anything other than green
        setVisible(!hideUnconstraintFeature || (getModelObject().indicator.isAffected()
                && getModelObject().indicator.getStatusColor().equals("green")));
    }

    private void actionAdd(AjaxRequestTarget aTarget)
    {
        if (isBlank((String) field.getModelObject()) && getTraits().isEnableRoleLabels()) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

        LinkWithRoleModel m = new LinkWithRoleModel();
        m.role = (String) field.getModelObject();
        int insertionPoint = findInsertionPoint(links);
        links.add(insertionPoint, m);
        state.setArmedSlot(getModelObject(), insertionPoint);

        // Need to re-render the whole form because a slot in another link editor might get unarmed
        aTarget.add(getOwner());
    }

    private int findInsertionPoint(List<LinkWithRoleModel> aLinks)
    {
        if (aLinks.isEmpty()) {
            return 0;
        }

        int i = aLinks.size();
        while (i > 0 && aLinks.get(i - 1).autoCreated) {
            i--;
        }

        return i;
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        if (isBlank((String) field.getModelObject()) && getTraits().isEnableRoleLabels()) {
            error("Must set slot label before changing!");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

        // Update the slot
        LinkWithRoleModel m = links.get(state.getArmedSlot());
        m.role = (String) field.getModelObject();
        links.set(state.getArmedSlot(), m); // avoid reordering

        aTarget.add(content);

        // Send event - but only if we set the label on a slot which was already filled/saved.
        // Unset slots only exist in the link editor and if we commit the change here, we
        // trigger a reload of the feature editors from the CAS which makes the unfilled slots
        // disappear and leaves behind an armed slot pointing to a removed slot.
        if (m.targetAddr != -1) {
            send(this, Broadcast.BUBBLE, new FeatureEditorValueChangedEvent(this, aTarget));
        }
    }

    private void actionDel(AjaxRequestTarget aTarget)
    {
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

        LinkWithRoleModel linkWithRoleModel = links.get(state.getArmedSlot());
        links.remove(state.getArmedSlot());
        state.clearArmedSlot();

        aTarget.add(content);

        send(this, BUBBLE, new LinkFeatureDeletedEvent(this, aTarget, linkWithRoleModel));
    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget, Item<LinkWithRoleModel> aItem)
    {
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject(), aItem.getIndex())) {
            state.clearArmedSlot();
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject(), aItem.getIndex());
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

    @OnEvent
    public void onAnnotationDeleted(AnnotationDeletedEvent aEvent)
    {
        // It could be that a slot filler was deleted - so just in case, we re-render ourself.
        aEvent.getRequestTarget().add(this);
    }

    @OnEvent
    public void onRenderSlotsEvent(RenderSlotsEvent aEvent)
    {
        // Redraw because it could happen that another slot is armed, replacing this.
        aEvent.getRequestHandler().add(this);
    }

    private LinkFeatureTraits getTraits()
    {
        AnnotationFeature feat = getModelObject().feature;
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(feat).orElseThrow();
        return (LinkFeatureTraits) fs.readTraits(feat);
    }
}
