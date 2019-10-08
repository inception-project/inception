/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.WeblichtRecommenderFactoryImpl.DEFAULT_WEBLICHT_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtFormat.PLAIN_TEXT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtFormat.TCF;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInput;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.AbstractTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.WeblichtRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.WeblichtRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;

public class WeblichtRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final Logger LOG = LoggerFactory.getLogger(WeblichtRecommender.class);

    private static final String MID_FORM = "form";
    private static final String MID_API_KEY = "apiKey";
    private static final String MID_URL = "url";
    private static final String MID_CHAIN = "chain";
    private static final String MID_LAST_KEY_UPDATE = "lastKeyUpdate";

    private @SpringBean WeblichtChainService chainService;
    private @SpringBean WeblichtRecommenderFactory toolFactory;

    private final WeblichtRecommenderTraits traits;
    private final IModel<WeblichtRecommenderTraits> traitsModel;

    private final ComboBox<String> urlField;
    private final TextField<String> apiKeyField;
    private final DateTextField lastKeyUpdateField;
    private final DropDownChoice<WeblichtFormat> formatField;
    private final ComboBox<String> languageField;
    private final WebMarkupContainer saveToAddChainAlert;
    private final WebMarkupContainer missingChainAlert;
    private final Label chainField;
    private final BootstrapFileInput uploadField;
    
    private final String oldApiKey;

    public WeblichtRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());
        oldApiKey = traits.getApiKey();
        traitsModel = CompoundPropertyModel.of(traits);
        Form<WeblichtRecommenderTraits> form =
                new Form<WeblichtRecommenderTraits>(MID_FORM, traitsModel)
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                
                if (traits.getApiKey() != null) {
                    if (!traits.getApiKey().equals(oldApiKey)) {
                        traits.setLastKeyUpdate(new Date());
                    }
                }
                else {
                    traits.setLastKeyUpdate(null);
                }
                
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };
        add(form);

        urlField = new ComboBox<>(MID_URL, asList(DEFAULT_WEBLICHT_URL));
        urlField.setRequired(true);
        urlField.add(new UrlValidator());
        urlField.setOutputMarkupId(true);
        form.add(urlField);

        apiKeyField = new TextField<>(MID_API_KEY);
        apiKeyField.setRequired(true);
        apiKeyField.setOutputMarkupId(true);
        form.add(apiKeyField);
        
        lastKeyUpdateField = new DateTextField(MID_LAST_KEY_UPDATE);
        lastKeyUpdateField.setOutputMarkupPlaceholderTag(true);
        lastKeyUpdateField.setEnabled(false);
        lastKeyUpdateField.add(visibleWhen(() -> traits.getApiKey() != null));
        form.add(lastKeyUpdateField);

        FileInputConfig config = new FileInputConfig();
        config.initialCaption("Import chain");
        config.allowedFileExtensions(asList("xml"));
        config.maxFileCount(1);
        config.showPreview(false);
        config.showUpload(true);
        config.removeIcon("<i class=\"fa fa-remove\"></i>");
        config.uploadIcon("<i class=\"fa fa-upload\"></i>");
        config.browseIcon("<i class=\"fa fa-folder-open\"></i>");
        uploadField = new BootstrapFileInput("upload", new ListModel<>(), config) {
            private static final long serialVersionUID = -7072183979425490246L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionUploadChain(aTarget);
            }
        };
        uploadField.add(visibleWhen(() -> aRecommender.getObject() != null
                && aRecommender.getObject().getId() != null));
        add(uploadField);
        
        chainField = new Label(MID_CHAIN, LoadableDetachableModel.of(this::getChain));
        chainField.setOutputMarkupPlaceholderTag(true);
        chainField.add(visibleWhen(() -> getModelObject().getId() != null));
        add(chainField);
        
        saveToAddChainAlert = new WebMarkupContainer("saveToAddChainAlert");
        saveToAddChainAlert.setOutputMarkupPlaceholderTag(true);
        saveToAddChainAlert.add(visibleWhen(() -> getModelObject().getId() == null));
        add(saveToAddChainAlert);
        
        missingChainAlert = new WebMarkupContainer("missingChainAlert");
        missingChainAlert.setOutputMarkupPlaceholderTag(true);
        missingChainAlert.add(visibleWhen(this::isChainMissing));
        add(missingChainAlert);
        
        languageField = new ComboBox<>("chainInputLanguage", asList("unknown", "de", "en"));
        languageField
                .add(LambdaBehavior.visibleWhen(() -> TCF.equals(traits.getChainInputFormat())));
        languageField.setOutputMarkupPlaceholderTag(true);
        languageField.setRequired(true);
        form.add(languageField);
        if (languageField.getModelObject() == null) {
            languageField.setModelObject("unknown");
        }
        
        formatField = new BootstrapSelect<WeblichtFormat>("chainInputFormat",
                asList(WeblichtFormat.values()), new EnumChoiceRenderer<>(this));
        formatField.setRequired(true);
        formatField.add(new LambdaAjaxFormComponentUpdatingBehavior("change",_target -> 
                _target.add(languageField)));
        form.add(formatField);
        if (formatField.getModelObject() == null) {
            formatField.setModelObject(PLAIN_TEXT);
        }
    }

    @Override
    public Recommender getModelObject()
    {
        return (Recommender) getDefaultModelObject();
    }
    
    private boolean isChainMissing()
    {
        if (getModelObject().getId() == null) {
            return false;
        }
        
        return !chainService.existsChain(getModelObject());
    }
    
    private String getChain()
    {
        return chainService.getChain(getModelObject()).map(WeblichtChain::getName)
                .orElse(getString("noChain"));
    }

    private void actionUploadChain(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(chainField, missingChainAlert);
        
        for (FileUpload importedGazeteer : uploadField.getModelObject()) {
            WeblichtChain chain = new WeblichtChain();
            chain.setName(importedGazeteer.getClientFileName());
            chain.setRecommender(getModelObject());
            
            // Make sure there is only one chain ever
            Optional<WeblichtChain> optChain = chainService.getChain(getModelObject());
            if (optChain.isPresent()) {
                try {
                    chainService.deleteChain(optChain.get());
                }
                catch (IOException e) {
                    LOG.error("Error removing existing chain", e);
                    error("Error removing existing chain: " + e.getMessage());
                }
            }
            
            try (InputStream is = importedGazeteer.getInputStream()) {
                chainService.createOrUpdateChain(chain);
                chainService.importChainFile(chain, is);
                success("Imported chain: [" + chain.getName() + "]");
            }
            catch (Exception e) {
                error("Error importing chain: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing chain", e);
            }
        }
    }
}
