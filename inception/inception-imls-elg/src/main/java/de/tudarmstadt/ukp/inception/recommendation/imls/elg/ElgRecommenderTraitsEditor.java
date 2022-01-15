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

import static java.time.Duration.ofMillis;
import java.io.IOException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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

public class ElgRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommendationEngineFactory<ElgRecommenderTraits> toolFactory;
    private @SpringBean ElgCatalogClient elgCatalogClient;
    private @SpringBean ElgAuthenticationClient elgAuthenticationClient;

    private final ElgRecommenderTraits traits;

    private ElgCatalogEntity searchResult;

    public ElgRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        add(new ElgSessionPanel("elgSession", aRecommender.map(Recommender::getProject)));

        Form<ElgRecommenderTraits> form = new Form<ElgRecommenderTraits>(MID_FORM,
                CompoundPropertyModel.of(Model.of(traits)))
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };
        form.setOutputMarkupPlaceholderTag(true);

        TextField<String> serviceName = new TextField<>("serviceName");
        serviceName.setOutputMarkupPlaceholderTag(true);
        serviceName.setRequired(true);
        form.add(serviceName);

        TextField<String> serviceUrlSync = new TextField<>("serviceUrlSync");
        serviceUrlSync.setOutputMarkupPlaceholderTag(true);
        serviceUrlSync.setRequired(true);
        form.add(serviceUrlSync);

        TextField<String> serviceUrlAsync = new TextField<>("serviceUrlAsync");
        serviceUrlAsync.setOutputMarkupPlaceholderTag(true);
        serviceUrlAsync.setRequired(true);
        form.add(serviceUrlAsync);

        ElgCatalogSearchField searchField = new ElgCatalogSearchField("search",
                new PropertyModel<ElgCatalogEntity>(this, "searchResult"));
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

                try {
                    traits.setServiceId(searchResult.getId());
                    traits.setServiceName(searchResult.getResourceName());
                    ElgCatalogEntityDetails details = elgCatalogClient
                            .details(searchResult.getDetailUrl());
                    traits.setServiceUrlAsync(details.getServiceInfo().getElgExecutionLocation());
                    traits.setServiceUrlSync(
                            details.getServiceInfo().getElgExecutionLocationSync());
                    aTarget.add(serviceName, serviceUrlSync, serviceUrlAsync);
                }
                catch (IOException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                    aTarget.addChildren(getPage(), IFeedback.class);
                }
            }
        });
        form.add(searchField);

        add(form);
    }
}
