/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.util.Optional;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class DisabledKBWarning
    extends Panel
{

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private IModel<ConceptFeatureTraits> featureTraits;
    private Project project;

    public DisabledKBWarning(String aId, IModel<AnnotationFeature> aFeatureModel,
        IModel<ConceptFeatureTraits> aTraitsModel)
    {
        super(aId);

        AnnotationFeature feature = aFeatureModel.getObject();
        project = feature.getProject();

        // If traits are not explicitly given, try to resolve them via featureSupportRegistry
        if (aTraitsModel == null) {
            FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
                .getFeatureSupport(aFeatureModel.getObject());
            featureTraits = Model.of(fs.readTraits(aFeatureModel.getObject()));
        }
        else {
            featureTraits = aTraitsModel;
        }

        String kbName = resolveKBName(featureTraits.getObject());

        WebMarkupContainer warning = new WebMarkupContainer("warning");
        add(warning);

        TooltipBehavior tip = new TooltipBehavior();
        warning.add(tip);
        tip.setOption("content", Options.asString(new StringResourceModel("disabledKbWarning", this)
            .setParameters(kbName, feature.getLayer().getUiName(), feature.getUiName())));
        tip.setOption("width", Options.asString("300px"));
    }

    public DisabledKBWarning(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        this(aId, aFeatureModel, null);

    }

    private String resolveKBName(ConceptFeatureTraits aTraits) {
        Optional<KnowledgeBase> kb = Optional.empty();
        if (aTraits != null && aTraits.getRepositoryId() != null) {
            kb = kbService.getKnowledgeBaseById(project,
                aTraits.getRepositoryId());
        }
        return kb.isPresent() ? kb.get().getName() : "unknown ID";
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        String repoId = featureTraits.getObject().getRepositoryId();
        setVisible(!(repoId == null || kbService.isKnowledgeBaseEnabled(project, repoId)));
    }

}
