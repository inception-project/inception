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
package de.tudarmstadt.ukp.inception.annotation.feature.number;

import static de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureTraits.EditorType.SPINNER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class NumberFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -2830709472810678708L;

    private static final String MID_FORM = "form";
    private static final String CID_EDITOR_TYPE = "editorType";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private IModel<Traits> traits;

    public NumberFeatureTraitsEditor(String aId,
            UimaPrimitiveFeatureSupport_ImplBase<NumberFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        // We cannot retain a reference to the actual SlotFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeature;
        traits = Model.of(readTraits());

        var form = new Form<Traits>(MID_FORM, CompoundPropertyModel.of(traits))
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

        var limited = new CheckBox("limited");
        limited.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                target -> target.add(form)));
        form.add(limited);

        var editorType = new DropDownChoice<NumberFeatureTraits.EditorType>(CID_EDITOR_TYPE);
        // editorType.setModel(PropertyModel.of(traits, "editorType"));
        editorType.setChoices(Arrays.asList(NumberFeatureTraits.EditorType.values()));
        editorType.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        editorType.add(visibleWhen(
                () -> traits.getObject().isLimited() && isEditorTypeSelectionPossible()));
        form.add(editorType);

        var minimum = new NumberTextField<>("minimum", clazz, options);
        minimum.add(visibleWhen(() -> traits.getObject().isLimited()));
        form.add(minimum);

        var maximum = new NumberTextField<>("maximum", clazz, options);
        maximum.add(visibleWhen(() -> traits.getObject().isLimited()));
        form.add(maximum);

        minimum.add(new LambdaAjaxFormComponentUpdatingBehavior("change", target -> {
            var min = new BigDecimal(traits.getObject().getMinimum().toString());
            var max = new BigDecimal(traits.getObject().getMaximum().toString());
            if (min.compareTo(max) > 0) {
                traits.getObject().setMaximum(traits.getObject().getMinimum());
            }
            target.add(form);
        }));

        maximum.add(new LambdaAjaxFormComponentUpdatingBehavior("change", target -> {
            var min = new BigDecimal(traits.getObject().getMinimum().toString());
            var max = new BigDecimal(traits.getObject().getMaximum().toString());
            if (min.compareTo(max) > 0) {
                traits.getObject().setMinimum(traits.getObject().getMaximum());
            }
            target.add(form);
        }));
    }

    /**
     * Checks if current settings for number feature allow for a selection of editor type. Radio
     * button editor can be used if the difference between maximum and minimum is smaller than 12
     * and the number feature is of type integer.
     */
    private boolean isEditorTypeSelectionPossible()
    {
        BigDecimal min = new BigDecimal(traits.getObject().getMinimum().toString());
        BigDecimal max = new BigDecimal(traits.getObject().getMaximum().toString());
        return max.subtract(min).compareTo(new BigDecimal(12)) < 0
                && feature.getObject().getType().equals(CAS.TYPE_NAME_INTEGER);
    }

    @SuppressWarnings("unchecked")
    private UimaPrimitiveFeatureSupport_ImplBase<NumberFeatureTraits> getFeatureSupport()
    {
        return (UimaPrimitiveFeatureSupport_ImplBase<NumberFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }

    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link NumberFeatureTraits}} to the the UI traits model ({@link NumberFeatureTraits}).
     */
    private Traits readTraits()
    {
        var result = new Traits();

        var t = getFeatureSupport().readTraits(feature.getObject());

        result.setLimited(t.isLimited());
        result.setMinimum(t.getMinimum());
        result.setMaximum(t.getMaximum());
        result.setEditorType(t.getEditorType());

        return result;
    }

    /**
     * Transfer the values from the UI traits model ({@link NumberFeatureTraits})to the actual
     * traits model {{@link NumberFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        var t = new NumberFeatureTraits();

        t.setLimited(traits.getObject().isLimited());
        t.setMinimum(traits.getObject().getMinimum());
        t.setMaximum(traits.getObject().getMaximum());
        t.setEditorType(
                isEditorTypeSelectionPossible() ? traits.getObject().getEditorType() : SPINNER);

        getFeatureSupport().writeTraits(feature.getObject(), t);
    }

    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link NumberFeatureTraits} via {@link #readTraits()} and {@link #writeTraits()}.
     */
    private static class Traits
        implements Serializable
    {
        private static final long serialVersionUID = -7279313974832947832L;

        private boolean limited;
        private Number minimum;
        private Number maximum;
        private NumberFeatureTraits.EditorType editorType;

        public boolean isLimited()
        {
            return limited;
        }

        public void setLimited(boolean aLimited)
        {
            limited = aLimited;
        }

        public Number getMinimum()
        {
            return minimum;
        }

        public void setMinimum(Number aMinimum)
        {
            minimum = aMinimum;
        }

        public Number getMaximum()
        {
            return maximum;
        }

        public void setMaximum(Number aMaximum)
        {
            maximum = aMaximum;
        }

        public NumberFeatureTraits.EditorType getEditorType()
        {
            return editorType;
        }

        public void setEditorType(NumberFeatureTraits.EditorType aEditorType)
        {
            editorType = aEditorType;
        }
    }
}
