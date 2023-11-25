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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelCard;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class HfRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<HfRecommenderTraits> toolFactory;
    private @SpringBean HfHubClient hfHubClient;

    private final IModel<HfRecommenderTraits> traits;

    private HfModelCard searchResult;

    public HfRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        Form<HfRecommenderTraits> form = new Form<HfRecommenderTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);

        TextField<String> apiToken = new TextField<>("apiToken");
        form.add(apiToken);

        TextField<String> serviceName = new TextField<>("modelId");
        serviceName.setOutputMarkupPlaceholderTag(true);
        serviceName.setEnabled(false);
        serviceName.add(visibleWhen(() -> traits.getObject().getModelId() != null));
        form.add(serviceName);

        Label serviceDescription = new Label("serviceDescription");
        serviceDescription.setDefaultModel(LoadableDetachableModel.of(this::getServiceDescription));
        serviceDescription.setOutputMarkupPlaceholderTag(true);
        serviceDescription.add(visibleWhen(() -> traits.getObject().getModelId() != null));
        form.add(serviceDescription);

        var catalogLink = new ExternalLink("catalogLink",
                LoadableDetachableModel.of(this::getCatalogLink));
        catalogLink.add(visibleWhen(() -> traits.getObject().getModelId() != null));
        form.add(catalogLink);

        var changeService = new LambdaAjaxLink("changeService", this::actionChangeService);
        changeService.add(visibleWhen(() -> traits.getObject().getModelId() != null));
        form.add(changeService);

        HfModelSearchField searchField = new HfModelSearchField("search",
                new PropertyModel<HfModelCard>(this, "searchResult"));
        searchField.add(visibleWhen(() -> traits.getObject().getModelId() == null));
        searchField.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -8944946839865527412L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
            {
                super.updateAjaxAttributes(aAttributes);
                aAttributes.setThrottlingSettings(new ThrottlingSettings(ofMillis(250), true));
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                actionSearchUpdated(aTarget);
            }
        });
        form.add(searchField);

        add(form);
    }

    private String getCatalogLink()
    {
        if (traits.getObject() == null) {
            return null;
        }

        return "https://huggingface.co/" + traits.getObject().getModelId();
    }

    private String getServiceDescription()
    {
        if (traits.getObject() == null) {
            return "No service selected";
        }

        try {
            return hfHubClient.details(traits.getObject().getModelId()).getModelId();
        }
        catch (IOException e) {
            return "Unable to obtain the service description: " + getRootCauseMessage(e);
        }
    }

    private void actionSearchUpdated(AjaxRequestTarget aTarget)
    {
        if (searchResult == null) {
            return;
        }

        HfRecommenderTraits t = traits.getObject();
        t.setModelId(searchResult.getModelId());
        aTarget.add(HfRecommenderTraitsEditor.this);
    }

    private void actionChangeService(AjaxRequestTarget aTarget)
    {
        searchResult = null;
        traits.setObject(toolFactory.createTraits());
        aTarget.add(this);
    }
}
