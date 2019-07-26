/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
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

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.StyledComboBox;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;

public class LinkFeatureEditor
    extends FeatureEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(LinkFeatureEditor.class);

    private static final long serialVersionUID = 7469241620229001983L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

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

        stateModel = aStateModel;
        actionHandler = aHandler;

        // Checks whether hide un-constraint feature is enabled or not
        hideUnconstraintFeature = getModelObject().feature.isHideUnconstraintFeature();

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
                        (List<LinkWithRoleModel>) LinkFeatureEditor.this.getModelObject().value)
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
            }
        });

        if (getModelObject().feature.getTagset() != null) {
            field = new StyledComboBox<Tag>("newRole", PropertyModel.of(this, "newRole"),
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
                    options.set("content",
                            ClassicKendoComboboxTextFeatureEditor.FUNCTION_FOR_TOOLTIP);
                    add(new TooltipBehavior("#" + field.getMarkupId() + "_listbox *[title]",
                            options)
                    {
                        private static final long serialVersionUID = -7207021885475073279L;

                        @Override
                        protected String $()
                        {
                            // REC: It takes a moment for the KendoDatasource to load the data and
                            // for the Combobox to render the hidden dropdown. I did not find
                            // a way to hook into this process and to get notified when the
                            // data is available in the dropdown, so trying to handle this
                            // with a slight delay hopeing that all is set up after 1 second.
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
                        LOG.trace("onInitialize() requesting datasource re-reading");
                        target.get().appendJavaScript(
                                String.format("var $w = %s; if ($w) { $w.dataSource.read(); }",
                                        KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD)));
                    }
                }
            };
        }
        else {
            field = new TextField<String>("newRole", PropertyModel.of(this, "newRole"));
        }

        field.add(LambdaBehavior.onConfigure(_this -> {
            // If a slot is armed, then load the slot's role into the dropdown
            FeatureState featureState = LinkFeatureEditor.this.getModelObject();
            AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();
            if (state.isSlotArmed() && featureState.feature.equals(state.getArmedFeature())) {
                List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) featureState.value;
                field.setModelObject(links.get(state.getArmedSlot()).role);
            }
            else {
                field.setModelObject("");
            }
        }));
        field.setOutputMarkupId(true);
        field.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
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
                        .equals(state.getArmedFeature())));
            }
        });

        // Allows user to update slot
        content.add(new LambdaAjaxLink("set", this::actionSet)
        {

            private static final long serialVersionUID = 7923695373085126646L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();
                setVisible(state.isSlotArmed() && LinkFeatureEditor.this.getModelObject().feature
                        .equals(state.getArmedFeature()));
            }
        });

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
                        .equals(state.getArmedFeature()));
            }
        });
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
        AnnotationFeature feat = getModelObject().feature;
        
        FeatureSupport<LinkFeatureTraits> fs = featureSupportRegistry.getFeatureSupport(feat);
        LinkFeatureTraits traits = fs.readTraits(feat);

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
        if (StringUtils.isBlank((String) field.getModelObject())) {
            error("Must set slot label before adding!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else {
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                    .getModelObject().value;
            AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

            LinkWithRoleModel m = new LinkWithRoleModel();
            m.role = (String) field.getModelObject();
            links.add(m);
            state.setArmedSlot(LinkFeatureEditor.this.getModelObject().feature, links.size() - 1);

            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    private void actionSet(AjaxRequestTarget aTarget)
    {
        @SuppressWarnings("unchecked")
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

        // Update the slot
        LinkWithRoleModel m = links.get(state.getArmedSlot());
        m.role = (String) field.getModelObject();
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
        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                .getModelObject().value;
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

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
        AnnotatorState state = LinkFeatureEditor.this.stateModel.getObject();

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
}
