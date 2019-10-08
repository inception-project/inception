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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.getObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.LappsGridRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.LappsGridRecommenderFactory;

public class LappsGridRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final Logger LOG = LoggerFactory.getLogger(LappsGridRecommender.class);

    public static String NER_LAYER = NamedEntity.class.getName();
    public static String NER_FEATURE = "value";
    public static String POS_LAYER = POS.class.getName();
    public static String POS_FEATURE = "PosValue";

    private static final String MID_FORM = "form";
    private static final String MID_SERVICES = "service";
    private static final String MID_URL = "url";

    private @SpringBean LappsGridRecommenderFactory toolFactory;

    private final LappsGridRecommenderTraits traits;
    private final IModel<LappsGridRecommenderTraits> traitsModel;

    private DropDownChoice<LappsGridService> servicesDropDown;
    private TextField<String> urlField;

    public LappsGridRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());
        traitsModel = CompoundPropertyModel.of(traits);
        Form<LappsGridRecommenderTraits> form =
                new Form<LappsGridRecommenderTraits>(MID_FORM, traitsModel)
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };

        urlField = new TextField<>(MID_URL);
        urlField.setRequired(true);
        urlField.add(new UrlValidator());
        urlField.setOutputMarkupId(true);
        form.add(urlField);

        servicesDropDown = new BootstrapSelect<>(MID_SERVICES);
        servicesDropDown.setModel(Model.of());
        servicesDropDown.setChoices(LoadableDetachableModel.of(this::getPredefinedServicesList));
        servicesDropDown.setChoiceRenderer(new ChoiceRenderer<>("description"));
        servicesDropDown.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            LappsGridService selection = servicesDropDown.getModelObject();
            if (selection != null) {
                traits.setUrl(selection.getUrl());
            }
            t.add(urlField);
        }));
        form.add(servicesDropDown);

        add(form);
    }

    private List<LappsGridService> getPredefinedServicesList()
    {
        Map<String, List<LappsGridService>> predefinedServices = loadPredefinedServicesData();

        String layer = getModelObject().getLayer().getName();
        String feature = getModelObject().getFeature().getName();

        if (NER_LAYER.equals(layer) && NER_FEATURE.equals(feature)) {
            return predefinedServices.get("ner");
        } else if (POS_LAYER.equals(layer) && POS_FEATURE.equals(feature)) {
            return predefinedServices.get("pos");
        } else {
            LOG.error("Wrong layer/feature configuration for LappsGridRecommender: [{}] - [{}]",
                      layer, feature);
            return Collections.emptyList();
        }
    }

    private Map<String, List<LappsGridService>> loadPredefinedServicesData()
    {
        try (InputStream is = getClass().getResourceAsStream("services.json")) {
            TypeReference<Map<String, List<LappsGridService>>> typeRef =
                    new TypeReference<Map<String, List<LappsGridService>>>() {};
            return getObjectMapper().readValue(is, typeRef);
        } catch (IOException e) {
            LOG.error("Could not load predefined services file!", e);
            return Collections.emptyMap();
        }
    }
}
