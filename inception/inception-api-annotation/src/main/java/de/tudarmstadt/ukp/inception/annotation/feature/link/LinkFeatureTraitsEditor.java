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

import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.DEFAULT_LINK_DIFF_MODE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.DEFAULT_LINK_MULTIPLICITY_MODE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_ONE_ROLE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.multiselect.MultiSelect;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class LinkFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 2129000875921279514L;

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private IModel<Traits> traits;

    public LinkFeatureTraitsEditor(String aId, LinkFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);

        // We cannot retain a reference to the actual SlotFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeatureModel;

        traits = Model.of(readTraits());

        var form = new Form<Traits>(MID_FORM, CompoundPropertyModel.of(traits))
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                // when saving reset the selected tagset if role labels are not enabled
                if (!traits.getObject().isEnableRoleLabels()) {
                    feature.getObject().setTagset(null);
                }
                writeTraits();
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);

        var defaultSlots = new MultiSelect<Tag>("defaultSlots")
        {
            private static final long serialVersionUID = 8231304829756188352L;

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                // aBehavior.setOption("placeholder", Options.asString(getString("placeholder")));
                aBehavior.setOption("filter", Options.asString("contains"));
                aBehavior.setOption("autoClose", false);
            }
        };
        defaultSlots.setChoices(LoadableDetachableModel.of(this::listTags));
        defaultSlots.setChoiceRenderer(new ChoiceRenderer<>("name"));
        defaultSlots.add(visibleWhen(() -> traits.getObject().isEnableRoleLabels()
                && feature.getObject().getTagset() != null));
        form.add(defaultSlots);

        var enableRoleLabels = new CheckBox("enableRoleLabels");
        enableRoleLabels.setModel(PropertyModel.of(traits, "enableRoleLabels"));
        enableRoleLabels.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", target -> target.add(form)));
        form.add(enableRoleLabels);

        var tagset = new DropDownChoice<TagSet>("tagset");
        tagset.setOutputMarkupPlaceholderTag(true);
        tagset.setChoiceRenderer(new ChoiceRenderer<>("name"));
        tagset.setNullValid(true);
        tagset.setModel(PropertyModel.of(aFeatureModel, "tagset"));
        tagset.setChoices(LoadableDetachableModel
                .of(() -> annotationService.listTagSets(aFeatureModel.getObject().getProject())));
        tagset.add(new LambdaAjaxFormComponentUpdatingBehavior("change", target -> {
            traits.getObject().setDefaultSlots(new ArrayList<>());
            target.add(form);
        }));
        tagset.add(visibleWhen(traits.map(Traits::isEnableRoleLabels)));
        form.add(tagset);

        var compareMode = new DropDownChoice<LinkFeatureMultiplicityMode>("compareMode",
                asList(LinkFeatureMultiplicityMode.values()), new EnumChoiceRenderer<>(this));
        compareMode.setOutputMarkupPlaceholderTag(true);
        compareMode.add(visibleWhen(traits.map(Traits::isEnableRoleLabels)));
        form.add(compareMode);

        var diffMode = new DropDownChoice<LinkFeatureDiffMode>("diffMode",
                asList(LinkFeatureDiffMode.values()), new EnumChoiceRenderer<>(this));
        diffMode.setOutputMarkupPlaceholderTag(true);
        form.add(diffMode);
    }

    private List<Tag> listTags()
    {
        if (feature.getObject().getTagset() != null) {
            return annotationService.listTags(feature.getObject().getTagset());
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link LinkFeatureTraits}} to the the UI traits model ({@link Traits}).
     */
    private Traits readTraits()
    {
        var result = new Traits();

        var t = getFeatureSupport().readTraits(feature.getObject());

        // Add any tags from the tagset which are default slots to the traits editor model
        listTags().stream().filter(tag -> t.getDefaultSlots().contains(tag.getId()))
                .forEach(result.getDefaultSlots()::add);

        result.setEnableRoleLabels(t.isEnableRoleLabels());
        result.setCompareMode(t.getMultiplicityMode());
        result.setDiffMode(t.getDiffMode());

        return result;
    }

    /**
     * Transfer the values from the UI traits model ({@link Traits}) to the actual traits model
     * {{@link LinkFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        var t = new LinkFeatureTraits();

        var enableRoleLabels = traits.getObject().isEnableRoleLabels();

        t.setEnableRoleLabels(enableRoleLabels);
        t.setDiffMode(traits.getObject().getDiffMode());

        if (enableRoleLabels) {
            // only set default slot values for tagsets if the role labels are enabled
            traits.getObject().getDefaultSlots().stream().map(tag -> tag.getId())
                    .forEach(t.getDefaultSlots()::add);
            t.setMultiplicityMode(traits.getObject().getCompareMode());
        }
        else {
            t.setMultiplicityMode(MULTIPLE_TARGETS_ONE_ROLE);
        }

        getFeatureSupport().writeTraits(feature.getObject(), t);
    }

    private LinkFeatureSupport getFeatureSupport()
    {
        return (LinkFeatureSupport) featureSupportRegistry.getExtension(featureSupportId)
                .orElseThrow();
    }

    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link LinkFeatureTraits} via {@link LinkFeatureTraitsEditor#readTraits()} and
     * {@link LinkFeatureTraitsEditor#writeTraits()}.
     */
    private static class Traits
        implements Serializable
    {
        private static final long serialVersionUID = 5804584375190949088L;

        private List<Tag> defaultSlots = new ArrayList<>();
        private boolean enableRoleLabels;
        private LinkFeatureMultiplicityMode compareMode = DEFAULT_LINK_MULTIPLICITY_MODE;
        private LinkFeatureDiffMode diffMode = DEFAULT_LINK_DIFF_MODE;

        public List<Tag> getDefaultSlots()
        {
            return defaultSlots;
        }

        public void setDefaultSlots(List<Tag> aDefaultSlots)
        {
            defaultSlots = aDefaultSlots;
        }

        public boolean isEnableRoleLabels()
        {
            return enableRoleLabels;
        }

        public void setEnableRoleLabels(boolean aEnableRoleLabels)
        {
            enableRoleLabels = aEnableRoleLabels;
        }

        public LinkFeatureMultiplicityMode getCompareMode()
        {
            return compareMode;
        }

        public void setCompareMode(LinkFeatureMultiplicityMode aCompareMode)
        {
            compareMode = aCompareMode;
        }

        public void setDiffMode(LinkFeatureDiffMode aDiffMode)
        {
            diffMode = aDiffMode;
        }

        public LinkFeatureDiffMode getDiffMode()
        {
            return diffMode;
        }
    }
}
