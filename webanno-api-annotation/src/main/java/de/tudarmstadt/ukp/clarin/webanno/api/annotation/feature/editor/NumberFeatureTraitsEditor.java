/*
 * Copyright 2019
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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class NumberFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -2830709472810678708L;
    
    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private IModel<Traits> traits;
    
    public NumberFeatureTraitsEditor(String aId, PrimitiveUimaFeatureSupport aFS,
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
            private static final long serialVersionUID = 4456748721289266655L;
        
            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        form.add(visibleWhen(() -> traits.getObject().isLimited()
                && feature.getObject().getTagset() == null));
        add(form);
    
        Class clazz = Integer.class;
        Options options = new Options();
        
        switch (feature.getObject().getType()) {
        case CAS.TYPE_NAME_INTEGER: {
            clazz = Integer.class;
            options.set("format", "'n0'");
            break;
        }
        case CAS.TYPE_NAME_FLOAT: {
            clazz = Float.class;
            break;
        }
        }
    
        NumberTextField minimum = new NumberTextField<>("minimum", clazz, options);
        minimum.setModel(PropertyModel.of(traits, "minimum"));
        form.add(minimum);
    
        NumberTextField maximum = new NumberTextField<>("maximum", clazz, options);
        maximum.setModel(PropertyModel.of(traits, "maximum"));
        form.add(maximum);
    
        minimum.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
            target -> {
                BigDecimal min = new BigDecimal(traits.getObject().getMinimum().toString());
                BigDecimal max = new BigDecimal(traits.getObject().getMaximum().toString());
                if (min.compareTo(max) > 0) {
                    traits.getObject().setMaximum(traits.getObject().getMinimum());
                }
                target.add(form);
            }));
    
        maximum.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
            target -> {
                BigDecimal min = new BigDecimal(traits.getObject().getMinimum().toString());
                BigDecimal max = new BigDecimal(traits.getObject().getMaximum().toString());
                if (min.compareTo(max) > 0) {
                    traits.getObject().setMinimum(traits.getObject().getMaximum());
                }
                target.add(form);
            }));
    
        CheckBox multipleRows = new CheckBox("limited");
        multipleRows.setModel(PropertyModel.of(traits, "limited"));
        multipleRows.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
            target -> target.add(form)));
        add(multipleRows);
    }
    
    private PrimitiveUimaFeatureSupport getFeatureSupport()
    {
        return featureSupportRegistry.getFeatureSupport(featureSupportId);
    }
    
    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link NumberFeatureTraits}} to the the UI traits model ({@link NumberFeatureTraits}).
     */
    private Traits readTraits()
    {
        Traits result = new Traits();
        
        NumberFeatureTraits t = getFeatureSupport().readNumberFeatureTraits(feature.getObject());
        
        result.setLimited(t.isLimited());
        result.setMinimum(t.getMinimum());
        result.setMaximum(t.getMaximum());
        
        return result;
    }
    
    /**
     * Transfer the values from the UI traits model ({@link NumberFeatureTraits})to the actual
     * traits model {{@link NumberFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        NumberFeatureTraits t = new NumberFeatureTraits();
        
        t.setLimited(traits.getObject().isLimited());
        t.setMinimum(traits.getObject().getMinimum());
        t.setMaximum(traits.getObject().getMaximum());
        
        getFeatureSupport().writeNumberFeatureTraits(feature.getObject(), t);
    }
    
    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link NumberFeatureTraits} via {@link #readTraits()} and
     * {@link #writeTraits()}.
     */
    private static class Traits implements Serializable
    {
        private static final long serialVersionUID = -7279313974832947832L;
    
        private boolean limited;
        private Number minimum;
        private Number maximum;
    
        public boolean isLimited()
        {
            return limited;
        }
    
        public void setLimited(boolean limited)
        {
            this.limited = limited;
        }
    
        public Number getMinimum()
        {
            return minimum;
        }
    
        public void setMinimum(Number minimum)
        {
            this.minimum = minimum;
        }
    
        public Number getMaximum()
        {
            return maximum;
        }
    
        public void setMaximum(Number maximum)
        {
            this.maximum = maximum;
        }
    }
}
