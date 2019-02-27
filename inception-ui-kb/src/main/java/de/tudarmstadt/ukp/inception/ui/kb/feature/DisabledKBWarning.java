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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class DisabledKBWarning
    extends Panel
{

    private @SpringBean FactLinkingService factLinkingService;
    private @SpringBean KnowledgeBaseService kbService;

    private ConceptFeatureTraits featureTraits;
    private Project project;

    public DisabledKBWarning(String aId, IModel<AnnotationFeature> aModel)
    {
        super(aId);

        AnnotationFeature feature = aModel.getObject();
        project = feature.getProject();
        featureTraits = factLinkingService.getFeatureTraits(project);

        String kbName = resolveKBName(featureTraits);

        WebMarkupContainer warning = new WebMarkupContainer("warning");
        add(warning);

        TooltipBehavior tip = new TooltipBehavior();
        warning.add(tip);
        tip.setOption("content", Options.asString(
            new StringResourceModel("disabledKbWarning", this).setParameters(kbName)
                .getString()));
        tip.setOption("width", Options.asString("300px"));

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
        String repoId = featureTraits.getRepositoryId();
        setVisible(!(repoId == null || kbService.isKnowledgeBaseAvailable(project, repoId)));
    }

}
