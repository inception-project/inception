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

import java.util.Optional;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class DisabledKBWarning
    extends GenericPanel<AnnotationFeature>
{
    private static final long serialVersionUID = -4673760000245492439L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private final IModel<String> repositoryIdModel;

    public DisabledKBWarning(String aId, IModel<AnnotationFeature> aFeatureModel,
            IModel<String> aRepositoryIdModel)
    {
        super(aId, aFeatureModel);

        repositoryIdModel = aRepositoryIdModel;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var feature = getModel().getObject();

        var kbName = repositoryIdModel.map(this::resolveKBName).getObject();

        var warning = new WebMarkupContainer("warning");
        add(warning);

        var tip = new TooltipBehavior();
        warning.add(tip);
        tip.setOption("content",
                Options.asString(new StringResourceModel("disabledKbWarning", this)
                        .setParameters(kbName, feature.getLayer().getUiName(), feature.getUiName())
                        .getObject()));
        tip.setOption("width", Options.asString("300px"));
    }

    private String resolveKBName(String aRepositoryId)
    {
        Optional<KnowledgeBase> kb = Optional.empty();
        if (aRepositoryId != null) {
            kb = kbService.getKnowledgeBaseById(getModel().getObject().getProject(), aRepositoryId);
        }
        return kb.isPresent() ? kb.get().getName() : "unknown ID";
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        String repoId = repositoryIdModel.getObject();
        setVisible(!(repoId == null
                || kbService.isKnowledgeBaseEnabled(getModel().getObject().getProject(), repoId)));
    }
}
