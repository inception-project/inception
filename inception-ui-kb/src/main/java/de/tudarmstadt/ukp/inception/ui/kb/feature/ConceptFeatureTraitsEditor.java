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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ConceptFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 2129000875921279514L;
    
    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureTraitsEditor.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private IModel<AnnotationFeature> feature;
    private IModel<ConceptFeatureTraits> traits;
    
    public ConceptFeatureTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);
        
        feature = aFeatureModel;
        
        traits = Model.of(readTraits());

        Form<ConceptFeatureTraits> form = new Form<ConceptFeatureTraits>("form",
                CompoundPropertyModel.of(traits))
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };
        
        form.add(new DropDownChoice<KBHandle>("scope", LambdaModel.of(this::listConcepts),
                new ChoiceRenderer<>("uiLabel")));
        
        add(form);
    }
    
    private ConceptFeatureTraits readTraits()
    {
        ConceptFeatureTraits result = null;
        
        try {
            FeatureSupport fs = featureSupportRegistry.getFeatureSupport(feature.getObject());
            result = (ConceptFeatureTraits) fs.readTraits(feature.getObject());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
            error("Unable to read traits: " + ExceptionUtils.getRootCauseMessage(e));
            IPartialPageRequestHandler target = RequestCycle.get()
                    .find(IPartialPageRequestHandler.class);
            if (target != null) {
                target.addChildren(getPage(), IFeedback.class);
            }
        }
        
        if (result == null) {
            result = new ConceptFeatureTraits();
        }
        
        return result;
    }
    
    private void writeTraits()
    {
        try {
            FeatureSupport fs = featureSupportRegistry.getFeatureSupport(feature.getObject());
            fs.writeTraits(feature.getObject(), traits.getObject());
        }
        catch (IOException e) {
            LOG.error("Unable to write traits", e);
            error("Unable to write traits: " + ExceptionUtils.getRootCauseMessage(e));
            IPartialPageRequestHandler target = RequestCycle.get()
                    .find(IPartialPageRequestHandler.class);
            if (target != null) {
                target.addChildren(getPage(), IFeedback.class);
            }
        }
    }
    
    private List<KBHandle> listConcepts()
    {
        Project project = feature.getObject().getProject();

        List<KBHandle> concepts = new ArrayList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(project)) {
            for (KBHandle concept : kbService.listConcepts(kb, false)) {
                concepts.add(concept);
            }
        }
        
        return concepts;
    }
}
