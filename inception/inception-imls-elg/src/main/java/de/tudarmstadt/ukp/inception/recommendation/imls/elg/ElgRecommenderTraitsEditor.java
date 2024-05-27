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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
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
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgCatalogClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntity;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntityDetails;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class ElgRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<ElgRecommenderTraits> toolFactory;
    private @SpringBean ElgCatalogClient elgCatalogClient;
    private @SpringBean ElgAuthenticationClient elgAuthenticationClient;

    private final IModel<ElgRecommenderTraits> traits;

    private ElgCatalogEntity searchResult;

    public ElgRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);

        traits = CompoundPropertyModel.of(toolFactory.readTraits(aRecommender.getObject()));

        add(new ElgSessionPanel("elgSession", aRecommender.map(Recommender::getProject)));

        Form<ElgRecommenderTraits> form = new Form<ElgRecommenderTraits>(MID_FORM, traits)
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

        TextField<String> serviceName = new TextField<>("serviceName");
        serviceName.setOutputMarkupPlaceholderTag(true);
        // serviceName.setRequired(true);
        serviceName.setEnabled(false);
        serviceName.add(visibleWhen(() -> traits.getObject().getServiceId() > 0));
        form.add(serviceName);

        Label serviceDescription = new Label("serviceDescription");
        serviceDescription.setDefaultModel(LoadableDetachableModel.of(this::getServiceDescription));
        serviceDescription.setOutputMarkupPlaceholderTag(true);
        serviceDescription.add(visibleWhen(() -> traits.getObject().getServiceId() > 0));
        form.add(serviceDescription);

        ExternalLink catalogLink = new ExternalLink("catalogLink",
                LoadableDetachableModel.of(this::getCatalogLink));
        catalogLink.add(visibleWhen(() -> traits.getObject().getServiceId() > 0));
        form.add(catalogLink);

        LambdaAjaxLink changeService = new LambdaAjaxLink("changeService",
                this::actionChangeService);
        changeService.add(visibleWhen(() -> traits.getObject().getServiceId() > 0));
        form.add(changeService);

        TextField<String> serviceUrlSync = new TextField<>("serviceUrlSync");
        serviceUrlSync.setOutputMarkupPlaceholderTag(true);
        // serviceUrlSync.setRequired(true);
        serviceUrlSync.setVisibilityAllowed(getApplication().getConfigurationType() == DEVELOPMENT);
        form.add(serviceUrlSync);

        TextField<String> serviceUrlAsync = new TextField<>("serviceUrlAsync");
        serviceUrlAsync.setOutputMarkupPlaceholderTag(true);
        // serviceUrlAsync.setRequired(true);
        serviceUrlAsync
                .setVisibilityAllowed(getApplication().getConfigurationType() == DEVELOPMENT);
        form.add(serviceUrlAsync);

        form.add(new AbstractFormValidator()
        {
            private static final long serialVersionUID = 2515827429297928492L;

            @Override
            public void validate(Form<?> aForm)
            {
                if (isAllBlank(serviceUrlAsync.getModelObject(), serviceUrlSync.getModelObject())) {
                    error(serviceName, "invalidService");
                }
            }

            @Override
            public FormComponent<?>[] getDependentFormComponents()
            {
                return new FormComponent<?>[] { serviceName, serviceUrlSync, serviceUrlAsync };
            }
        });

        ElgCatalogSearchField searchField = new ElgCatalogSearchField("search",
                new PropertyModel<ElgCatalogEntity>(this, "searchResult"));
        searchField.add(visibleWhen(() -> traits.getObject().getServiceId() == 0));
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
                if (searchResult == null) {
                    return;
                }

                ElgCatalogEntityDetails details;
                try {
                    details = elgCatalogClient.details(searchResult.getDetailUrl());
                }
                catch (IOException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                    aTarget.addChildren(getPage(), IFeedback.class);
                    return;
                }

                if (details.getServiceInfo() == null) {
                    error("Service [" + searchResult.getResourceName()
                            + "] is not callable - please select another one");
                    aTarget.addChildren(getPage(), IFeedback.class);
                    return;
                }

                ElgRecommenderTraits t = traits.getObject();
                t.setServiceId(searchResult.getId());
                t.setServiceName(searchResult.getResourceName());
                t.setServiceUrlAsync(details.getServiceInfo().getElgExecutionLocation());
                t.setServiceUrlSync(details.getServiceInfo().getElgExecutionLocationSync());
                t.setServiceDetailsUrl(searchResult.getDetailUrl());
                aTarget.add(ElgRecommenderTraitsEditor.this);
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

        return "https://live.european-language-grid.eu/catalogue/tool-service/"
                + traits.getObject().getServiceId();
    }

    private String getServiceDescription()
    {
        if (traits.getObject() == null) {
            return "No service selected";
        }

        try {
            return elgCatalogClient.findServiceById(traits.getObject().getServiceId()) //
                    .map(ElgCatalogEntity::getDescription) //
                    .orElse("No description");
        }
        catch (IOException e) {
            return "Unable to obtain the service description: " + getRootCauseMessage(e);
        }
    }

    private void actionChangeService(AjaxRequestTarget aTarget)
    {
        searchResult = null;
        traits.setObject(toolFactory.createTraits());
        aTarget.add(this);
    }
}
