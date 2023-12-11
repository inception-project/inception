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
package de.tudarmstadt.ukp.inception.image.sidebar;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.image.feature.ImageFeatureSupport;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

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

        ListView<ImageHandle> images = new ListView<ImageHandle>("images")
        {
            private static final long serialVersionUID = -1203277069357712752L;

            @Override
            protected void populateItem(ListItem<ImageHandle> item)
            {
                item.add(new ExternalLink("open", item.getModelObject().getUrl()));
                LambdaAjaxLink jumpToLink = new LambdaAjaxLink("jumpTo",
                        _target -> actionJumpTo(_target, item.getModelObject()));
                item.add(jumpToLink);
                jumpToLink.add(new ExternalImage("image", item.getModelObject().getUrl()));
            }
        };
        images.setModel(LoadableDetachableModel.of(this::listImageUrls));

        mainContainer.add(images);
    }

    private List<ImageHandle> listImageUrls()
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
            for (AnnotationFeature feat : annotationService.listSupportedFeatures(layer)) {
                if (feat.getType().startsWith(ImageFeatureSupport.PREFIX)) {
                    imageFeatures.add(feat);
                }
            }
        }

        // Extract the URLs
        List<ImageHandle> images = new ArrayList<>();
        TypeSystem ts = cas.getTypeSystem();
        for (AnnotationFeature feat : imageFeatures) {
            Type t = getType(cas, feat.getLayer().getName());

            // We only consider images that are annotated at the text level
            if (!ts.subsumes(cas.getAnnotationType(), t)) {
                continue;
            }

            Feature f = t.getFeatureByBaseName(feat.getName());

            List<AnnotationFS> annotations = selectCovered(cas, t, state.getWindowBeginOffset(),
                    state.getWindowEndOffset());

            for (AnnotationFS anno : annotations) {
                String url = anno.getFeatureValueAsString(f);

                if (isNotBlank(url)) {
                    images.add(new ImageHandle(url, state.getDocument(), VID.of(anno),
                            anno.getBegin(), anno.getEnd()));
                }
            }
        }

        return images;
    }

    @OnEvent
    public void onRenderRequested(RenderRequestedEvent aEvent)
    {
        aEvent.getRequestHandler().add(mainContainer);
    }

    public void actionJumpTo(AjaxRequestTarget aTarget, ImageHandle aHandle)
    {
        try {
            AnnotatorState state = getModelObject();

            // Get the CAS
            CAS cas = getCasProvider().get();

            AnnotationFS fs = ICasUtil.selectAnnotationByAddr(cas, aHandle.getVid().getId());

            AnnotationLayer layer = annotationService.findLayer(state.getProject(), fs);
            if (SPAN_TYPE.equals(layer.getType())) {
                state.getSelection().selectSpan(aHandle.getVid(), cas, aHandle.getBegin(),
                        aHandle.getEnd());
            }
            else if (RELATION_TYPE.equals(layer.getType())) {
                RelationAdapter adapter = (RelationAdapter) annotationService.getAdapter(layer);
                AnnotationFS originFS = FSUtil.getFeature(fs, adapter.getSourceFeatureName(),
                        AnnotationFS.class);
                AnnotationFS targetFS = FSUtil.getFeature(fs, adapter.getTargetFeatureName(),
                        AnnotationFS.class);
                state.getSelection().selectArc(aHandle.getVid(), originFS, targetFS);
            }
            else {
                return;
            }

            actionShowSelectedDocument(aTarget, aHandle.getDocument(), aHandle.getBegin(),
                    aHandle.getEnd());
            aTarget.add((Component) getActionHandler());
        }
        catch (IOException e) {
            error("Unable to select annotation: " + e.getMessage());
            LOG.error("Unable to select annotation", e);
        }
    }

    private static class ImageHandle
        implements Serializable
    {
        private static final long serialVersionUID = 9032602311793328638L;

        private final String url;
        private final SourceDocument document;
        private final VID vid;
        private final int begin;
        private final int end;

        public ImageHandle(String aUrl, SourceDocument aDocument, VID aVid, int aBegin, int aEnd)
        {
            super();
            url = aUrl;
            document = aDocument;
            vid = aVid;
            begin = aBegin;
            end = aEnd;
        }

        public String getUrl()
        {
            return url;
        }

        public SourceDocument getDocument()
        {
            return document;
        }

        public VID getVid()
        {
            return vid;
        }

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }
    }
}
