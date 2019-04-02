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
package de.tudarmstadt.ukp.inception.image.sidebar;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.image.feature.ImageFeatureSupport;

public class ImageSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -6367010242201414871L;

    private static final Logger LOG = LoggerFactory.getLogger(ImageSidebar.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    final WebMarkupContainer mainContainer;

    public ImageSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        ListView<String> images = new ListView<String>("images")
        {
            private static final long serialVersionUID = -1203277069357712752L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                item.add(new ExternalImage("image", item.getModelObject()));
                item.add(new ExternalLink("open", item.getModelObject()));
            }
        };
        images.setModel(LoadableDetachableModel.of(this::listImageUrls));
        
        mainContainer.add(images);
    }
    
    private List<String> listImageUrls()
    {
        AnnotatorState state = getModelObject();
        Project project = state.getProject();
        
        // Get the CAS
        CAS cas;
        try {
            cas = getCasProvider().get();
        }
        catch (IOException e) {
            error("Unable to load CAS");
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        // Collect all image features
        List<AnnotationFeature> imageFeatures = new ArrayList<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(project)) {
            for (AnnotationFeature feat : annotationService.listAnnotationFeature(layer)) {
                if (feat.getType().startsWith(ImageFeatureSupport.PREFIX)) {
                    imageFeatures.add(feat);
                }
            }
        }

        // Extract the URLs
        List<String> urls = new ArrayList<>();
        TypeSystem ts = cas.getTypeSystem();
        for (AnnotationFeature feat : imageFeatures) {
            Type t = getType(cas, feat.getLayer().getName());
            
            // We only consider images that are annotated at the text level
            if (!ts.subsumes(cas.getAnnotationType(), t)) {
                continue;
            }
            
            Feature f = t.getFeatureByBaseName(feat.getName());
            
            List<AnnotationFS> annotations = selectCovered(cas, t,
                    state.getWindowBeginOffset(), state.getWindowEndOffset());
            
            for (AnnotationFS anno : annotations) {
                String url = anno.getFeatureValueAsString(f);
                
                if (isNotBlank(url)) {
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }
    
    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        aEvent.getRequestHandler().add(mainContainer);
    }
}
