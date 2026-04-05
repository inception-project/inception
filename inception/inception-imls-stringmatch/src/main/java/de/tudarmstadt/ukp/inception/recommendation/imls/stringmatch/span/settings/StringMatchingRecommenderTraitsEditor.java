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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.settings;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.GazetteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.Gazetteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.GazetteerEntry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class StringMatchingRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<StringMatchingRecommenderTraits> toolFactory;
    private @SpringBean GazetteerService gazetteerService;

    private final StringMatchingRecommenderTraits traits;

    private GazetteerList gazetteers;
    private BootstrapFileInputField uploadField;

    public StringMatchingRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        var form = new Form<StringMatchingRecommenderTraits>(MID_FORM,
                CompoundPropertyModel.of(Model.of(traits)))
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };
        add(form);

        var ignoreCase = new CheckBox("ignoreCase");
        ignoreCase.setOutputMarkupId(true);
        ignoreCase.add(LambdaBehavior.visibleWhen(getModel() //
                .map(Recommender::getFeature) //
                .map(this::isStringBasedFeature)));
        form.add(ignoreCase);

        form.add(new NumberTextField<>("minLength", Integer.class) //
                .setMinimum(1) //
                .setMaximum(500) //
                .setStep(1));

        form.add(new TextField<>("excludePattern", String.class));

        gazetteers = new GazetteerList("gazetteers",
                LoadableDetachableModel.of(this::listGazetteers));
        gazetteers.add(visibleWhen(getModel().map(Recommender::getId).isPresent()));
        add(gazetteers);

        var config = new FileInputConfig();
        config.initialCaption("Import gazetteers ...");
        config.allowedFileExtensions(asList("txt"));
        config.showPreview(false);
        config.showUpload(true);
        uploadField = new BootstrapFileInputField("upload", new ListModel<>(), config)
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                actionUploadGazetteer(aTarget);
            }
        };
        uploadField.add(visibleWhen(
                getModel().map(r -> r.getId() != null && isStringBasedFeature(r.getFeature()))));
        add(uploadField);
    }

    private boolean isStringBasedFeature(AnnotationFeature f)
    {
        return f.isVirtualFeature()
                || asList(TYPE_NAME_STRING, TYPE_NAME_STRING_ARRAY).contains(f.getType());
    }

    private void actionDeleteGazetteer(AjaxRequestTarget aTarget, Gazetteer aGazetteer)
        throws IOException
    {
        gazetteerService.deleteGazetteers(aGazetteer);

        aTarget.add(gazetteers);
    }

    private void actionUploadGazetteer(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(gazetteers);

        for (var importedGazetteer : uploadField.getModelObject()) {
            var gazetteer = new Gazetteer();
            gazetteer.setName(importedGazetteer.getClientFileName());
            gazetteer.setRecommender(getModelObject());

            // Make sure the gazetteer name is unique
            var n = 2;
            while (gazetteerService.existsGazetteer(gazetteer.getRecommender(),
                    gazetteer.getName())) {
                var baseName = getBaseName(importedGazetteer.getClientFileName());
                var extension = getExtension(importedGazetteer.getClientFileName());
                gazetteer.setName(baseName + ' ' + n + '.' + extension);
                n++;
            }

            try (var is = importedGazetteer.getInputStream()) {
                // Read bytes so we can validate the file before persisting the DB entity
                var bytes = IOUtils.toByteArray(is);

                // Validate by parsing
                var parsed = new ArrayList<GazetteerEntry>();
                try (var isForParse = new ByteArrayInputStream(bytes)) {
                    gazetteerService.parseGazetteer(gazetteer, isForParse, parsed);
                }

                // Persist the gazetteer (assigns id used for file path)
                gazetteerService.createOrUpdateGazetteer(gazetteer);

                // Import the validated bytes
                try (var isForImport = new ByteArrayInputStream(bytes)) {
                    gazetteerService.importGazetteerFile(gazetteer, isForImport);
                }

                success("Imported gazetteer: [" + gazetteer.getName() + "]");
            }
            catch (Exception e) {
                error("Error importing gazetteer: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing gazetteer", e);
                if (gazetteer.getId() != null) {
                    try {
                        gazetteerService.deleteGazetteers(gazetteer);
                    }
                    catch (IOException ex) {
                        LOG.error("Failed to delete incomplete gazetteer: {}", gazetteer.getName(),
                                ex);
                    }
                }
            }
        }
    }

    @Override
    public Recommender getModelObject()
    {
        return (Recommender) getDefaultModelObject();
    }

    private List<Gazetteer> listGazetteers()
    {
        var recommender = getModelObject();

        if (recommender != null && recommender.getId() != null) {
            return gazetteerService.listGazetteers(recommender);
        }
        else {
            return emptyList();
        }
    }

    public class GazetteerList
        extends WebMarkupContainer
    {
        private static final long serialVersionUID = -1L;

        private ListView<Gazetteer> gazetteerList;

        public GazetteerList(String aId, IModel<? extends List<Gazetteer>> aChoices)
        {
            super(aId, aChoices);

            setOutputMarkupPlaceholderTag(true);

            gazetteerList = new ListView<Gazetteer>("gazetteer", aChoices)
            {
                private static final long serialVersionUID = -1L;

                @Override
                protected void populateItem(ListItem<Gazetteer> aItem)
                {
                    var gazetteer = aItem.getModelObject();

                    aItem.add(new Label("name", aItem.getModelObject().getName()));

                    aItem.add(new LambdaAjaxLink("delete",
                            _target -> actionDeleteGazetteer(_target, gazetteer)));

                    aItem.add(new DownloadLink("download",
                            LoadableDetachableModel.of(() -> getGazetteerFile(gazetteer)),
                            gazetteer.getName()));
                }
            };
            add(gazetteerList);
        }

        private File getGazetteerFile(Gazetteer aGazetteer)
        {
            try {
                return gazetteerService.getGazetteerFile(aGazetteer);
            }
            catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }
    }
}
