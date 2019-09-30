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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.settings;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInput;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;

public class StringMatchingRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final Logger LOG = LoggerFactory
            .getLogger(StringMatchingRecommenderTraitsEditor.class);
    
    private static final long serialVersionUID = 1677442652521110324L;

    private @SpringBean GazeteerService gazeteerService;
    
    private GazeteerList gazeteers;
    private BootstrapFileInput uploadField;

    public StringMatchingRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        gazeteers = new GazeteerList("gazeteers", LoadableDetachableModel.of(this::listGazeteers));
        gazeteers.add(visibleWhen(() -> aRecommender.getObject() != null
                && aRecommender.getObject().getId() != null));
        add(gazeteers);
        
        FileInputConfig config = new FileInputConfig();
        config.initialCaption("Import gazeteers ...");
        config.allowedFileExtensions(asList("txt"));
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
                actionUploadGazeteer(aTarget);
            }
        };
        uploadField.add(visibleWhen(() -> aRecommender.getObject() != null
                && aRecommender.getObject().getId() != null));
        add(uploadField);
    }

    private void actionDeleteGazeteer(AjaxRequestTarget aTarget, Gazeteer aGazeteer)
        throws IOException
    {
        gazeteerService.deleteGazeteers(aGazeteer);
        
        aTarget.add(gazeteers);
    }
    
    private void actionUploadGazeteer(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(gazeteers);
        
        for (FileUpload importedGazeteer : uploadField.getModelObject()) {
            Gazeteer gazeteer = new Gazeteer();
            gazeteer.setName(importedGazeteer.getClientFileName());
            gazeteer.setRecommender(getModelObject());
            
            // Make sure the gazetter name is unique
            int n = 2;
            while (gazeteerService.existsGazeteer(gazeteer.getRecommender(), gazeteer.getName())) {
                String baseName = FilenameUtils.getBaseName(importedGazeteer.getClientFileName());
                String extension = FilenameUtils.getExtension(importedGazeteer.getClientFileName());
                gazeteer.setName(baseName + ' ' + n + '.' + extension);
                n++;
            }
            
            try (InputStream is = importedGazeteer.getInputStream()) {
                gazeteerService.createOrUpdateGazeteer(gazeteer);
                gazeteerService.importGazeteerFile(gazeteer, is);
                success("Imported gazeteer: [" + gazeteer.getName() + "]");
            }
            catch (Exception e) {
                error("Error importing gazeteer: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error importing gazeteer", e);
            }
        }
    }
    
    @Override
    public Recommender getModelObject()
    {
        return (Recommender) getDefaultModelObject();
    }

    private List<Gazeteer> listGazeteers()
    {
        Recommender recommender = getModelObject();

        if (recommender != null && recommender.getId() != null) {
            return gazeteerService.listGazeteers(recommender);
        }
        else {
            return emptyList();
        }
    }
    
    public class GazeteerList extends WebMarkupContainer
    {
        private static final long serialVersionUID = -2049981253344229438L;
        
        private ListView<Gazeteer> gazeteerList;
        
        public GazeteerList(String aId, IModel<? extends List<Gazeteer>> aChoices)
        {
            super(aId, aChoices);
            
            setOutputMarkupPlaceholderTag(true);
            
            gazeteerList = new ListView<Gazeteer>("gazeteer", aChoices) {
                private static final long serialVersionUID = 2827701590781214260L;

                @Override
                protected void populateItem(ListItem<Gazeteer> aItem)
                {
                    Gazeteer gazeteer = aItem.getModelObject();
                    
                    aItem.add(new Label("name", aItem.getModelObject().getName()));

                    aItem.add(new LambdaAjaxLink("delete",
                        _target -> actionDeleteGazeteer(_target, gazeteer)));

                    aItem.add(new DownloadLink("download",
                            LoadableDetachableModel.of(() -> getGazeteerFile(gazeteer)),
                            gazeteer.getName()));
                }
            };
            add(gazeteerList);
        }
        
        private File getGazeteerFile(Gazeteer aGazeteer)
        {
            try {
                return gazeteerService.getGazeteerFile(aGazeteer);
            }
            catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }
    }
}
