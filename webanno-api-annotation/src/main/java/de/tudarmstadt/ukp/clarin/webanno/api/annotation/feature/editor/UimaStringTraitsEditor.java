/*
 * Copyright 2018
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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.Serializable;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class UimaStringTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -9082045435380184514L;
    
    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private IModel<Traits> traits;
    
    public UimaStringTraitsEditor(String aId, PrimitiveUimaFeatureSupport aFS, 
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);
    
        // We cannot retain a reference to the actual SlotFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeature;
    
        traits = Model.of(readTraits());
    
        Form<Traits> form = new Form<Traits>(MID_FORM, CompoundPropertyModel.of(traits))
        {
            private static final long serialVersionUID = -3109239605783291123L;
        
            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        form.add(visibleWhen(() -> traits.getObject().isMultipleRows()
                && feature.getObject().getTagset() == null));
        add(form);
    
        NumberTextField collapsedRows = new NumberTextField<>("collapsedRows", Integer.class);
        collapsedRows.setModel(PropertyModel.of(traits, "collapsedRows"));
        collapsedRows.setMinimum(1);
        form.add(collapsedRows);
        
        NumberTextField expandedRows = new NumberTextField<>("expandedRows", Integer.class);
        expandedRows.setModel(PropertyModel.of(traits, "expandedRows"));
        expandedRows.setMinimum(1);
        form.add(expandedRows);
    
        CheckBox multipleRows = new CheckBox("multipleRows");
        multipleRows.setModel(PropertyModel.of(traits, "multipleRows"));
        multipleRows.add(new LambdaAjaxFormComponentUpdatingBehavior("change", 
            target -> target.add(form)));
        add(multipleRows);
        
        DropDownChoice<TagSet> tagset = new BootstrapSelect<>("tagset");
        tagset.setOutputMarkupPlaceholderTag(true);
        tagset.setOutputMarkupId(true);
        tagset.setChoiceRenderer(new ChoiceRenderer<>("name"));
        tagset.setNullValid(true);
        tagset.setModel(PropertyModel.of(aFeature, "tagset"));
        tagset.setChoices(LoadableDetachableModel.of(() -> annotationService
                .listTagSets(aFeature.getObject().getProject())));
        tagset.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
            target -> {
                target.add(multipleRows);
                target.add(form);
            }));
        add(tagset);
    }
    
    private PrimitiveUimaFeatureSupport getFeatureSupport()
    {
        return featureSupportRegistry.getFeatureSupport(featureSupportId);
    }
    
    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link UimaStringTraits}} to the the UI traits model ({@link UimaStringTraits}).
     */
    private Traits readTraits()
    {
        Traits result = new Traits();
    
        UimaStringTraits t = getFeatureSupport().readUimaStringTraits(feature.getObject());
        
        result.setMultipleRows(t.isMultipleRows());
        result.setCollapsedRows(t.getCollapsedRows());
        result.setExpandedRows(t.getExpandedRows());
                
        return result;
    }
    
    /**
     * Transfer the values from the UI traits model ({@link UimaStringTraits})to the actual
     * traits model {{@link UimaStringTraits}} and then store them.
     */
    private void writeTraits()
    {
        UimaStringTraits t = new UimaStringTraits();
        
        t.setMultipleRows(traits.getObject().isMultipleRows());
        t.setCollapsedRows(traits.getObject().getCollapsedRows());
        t.setExpandedRows(traits.getObject().getExpandedRows());
        
        getFeatureSupport().writeUimaStringTraits(feature.getObject(), t);
    }
    
    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link UimaStringTraits} via {@link #readTraits()} and
     * {@link #writeTraits()}.
     */
    private static class Traits implements Serializable
    {
        private static final long serialVersionUID = 350784828528183399L;
    
        private boolean multipleRows = false;
        private int collapsedRows = 1;
        private int expandedRows = 1;
    
        public boolean isMultipleRows()
        {
            return multipleRows;
        }
    
        public void setMultipleRows(boolean multipleRows)
        {
            this.multipleRows = multipleRows;
        }
    
        public int getCollapsedRows()
        {
            return collapsedRows;
        }
    
        public void setCollapsedRows(int collapsedRows)
        {
            this.collapsedRows = collapsedRows;
        }
    
        public int getExpandedRows()
        {
            return expandedRows;
        }
    
        public void setExpandedRows(int expandedRows)
        {
            this.expandedRows = expandedRows;
        }
    }
}
