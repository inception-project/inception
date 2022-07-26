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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.multiselect.lazy.MultiSelect;
import com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

public class MultiValueConceptFeatureEditor
    extends ConceptFeatureEditor_ImplBase
{
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private MultiSelect<KBHandle> focusComponent;

    public MultiValueConceptFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, IModel<AnnotatorState> aStateModel,
            AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel, aStateModel, aHandler);

        focusComponent = new KBHandleMultiSelect("value", aHandler, aStateModel);
        add(focusComponent);
    }

    @Override
    public FormComponent getFocusComponent()
    {
        return focusComponent;
    }

    @Override
    protected MultiValueConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (MultiValueConceptFeatureTraits) fs.readTraits(aAnnotationFeature);
    }

    private final class KBHandleMultiSelect
        extends MultiSelect<KBHandle>
    {
        private final AnnotationActionHandler handler;
        private final IModel<AnnotatorState> stateModel;
        private static final long serialVersionUID = 7769511105678209462L;

        private KBHandleMultiSelect(String aId, AnnotationActionHandler aHandler,
                IModel<AnnotatorState> aStateModel)
        {
            super(aId, new ChoiceRenderer<>("uiLabel", "identifier"));
            handler = aHandler;
            stateModel = aStateModel;
        }

        @Override
        protected List<KBHandle> getChoices(String aInput)
        {
            if (aInput == null || aInput.length() == 0) {
                return new ArrayList<>(getModelObject());
            }

            var candidates = getCandidates(stateModel, handler, aInput);

            var selected = new ArrayList<>(getModelObject());
            selected.removeAll(candidates);

            var choices = new ArrayList<KBHandle>();
            choices.addAll(candidates);
            choices.addAll(selected);

            return choices;
        }

        @Override
        public void onConfigure(JQueryBehavior aBehavior)
        {
            super.onConfigure(aBehavior);

            aBehavior.setOption("autoWidth", true);
            aBehavior.setOption("animation", false);
            aBehavior.setOption("delay", 250);
            aBehavior.setOption("height", 300);
            // These three settings should avoid a query when simply clicking into the multiselect
            // field, but they seem to have no effect
            // aBehavior.setOption("autoBind", false);
            // aBehavior.setOption("minLength", 1);
            // aBehavior.setOption("enforceMinLength", true);
            aBehavior.setOption("tagTemplate",
                    Options.asString(
                            "<span title=\"#: data.description + '\\n\\n' + data.identifier #\" "
                                    + "onmouseover=\"javascript:applyTooltip(this)\">"
                                    + "#: data.uiLabel #</span>\n"));
        }

        @Override
        protected IJQueryTemplate newTemplate()
        {
            return new KBHandleTemplate();
        }
    }
}
