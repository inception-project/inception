/*
 * Copyright 2017
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

import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ConceptFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7763348613632105600L;

    private Component focusComponent;
    
    private @SpringBean KnowledgeBaseService kbService;
    
    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel, 
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aActionHandler)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        // Checks whether hide un-constraint feature is enabled or not
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(focusComponent = createFieldComboBox(aStateModel.getObject(), aActionHandler));
    }

    private DropDownList<KBHandle> createFieldComboBox(AnnotatorState aState, 
            AnnotationActionHandler aActionHandler)
    {
        DropDownList<KBHandle> field = new DropDownList<>("value", LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;    
            String identifier = feat.getType().substring(ConceptFeatureSupport.PREFIX.length());

            // retrieve handles from all knowledge bases
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                if (!identifier.isEmpty()) {
                    KBConcept concept = kbService.readConcept(kb, identifier);
                    if (concept != null) {
                        handles.addAll(kbService.listInstances(kb, concept.getIdentifier(), true, 
                                aState, aActionHandler));
                }
                // List instances of all concepts
                else {
                    handles.addAll(kbService.listInstances(kb, null, true, aState,
                        aActionHandler));
                }
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));

        // Ensure that markup IDs of feature editor focus components remain constant across
        // refreshes of the feature editor panel. This is required to restore the focus.
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }
    
    private List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri,
        boolean aAll, AnnotatorState aState, AnnotationActionHandler aActionHandler)
    {
        if (kb.isSupportConceptLinking()) {
            if (aConceptIri != null) {
                return kbService.listInstances(kb, aConceptIri, aAll);
            }
            // List instances of all concepts
            else {
                return listLinkingInstances(kb, null, false, aAll, aState, aActionHandler);
            }
        }
        else {
            IRI conceptIri = SimpleValueFactory.getInstance().createIRI(aConceptIri);
            return kbService.list(kb, conceptIri, false, aAll);
        }
    }
    
    private List<KBHandle> listLinkingInstances(KnowledgeBase kb, IRI conceptIri,
            boolean aIncludeInferred, boolean aAll, AnnotatorState aState, 
            AnnotationActionHandler aActionHandler)
    {
        List<KBHandle> resultList = kbService.read(kb, (conn) -> {
            List<Entity> candidates = extensionRegistry.fireDisambiguate(kb, conceptIri, aState, 
                    aActionHandler);
            List<KBHandle> handles = new ArrayList<>();

            for (Entity c: candidates) {
                String id = c.getIRI();
                String label = c.getLabel();

                if (!id.contains(":") || (!aAll && startsWithAny(id, IMPLICIT_NAMESPACES))) {
                    continue;
                }

                KBHandle handle = new KBHandle(id, label);
                if (!handles.contains(handle)) {
                    handles.add(handle);
                }
            }

            return handles;
        });

        return resultList;
    }
}
