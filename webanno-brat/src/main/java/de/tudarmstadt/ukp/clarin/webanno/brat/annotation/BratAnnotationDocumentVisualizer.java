/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.annotation.Resource;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.dao.DataRetrievalFailureException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Displays a BRAT visualisation and fills it with data from an {@link AnnotationDocument}. We do
 * not use a CAS as model object because the CAS is large and does not serialize well. It is easier
 * to drive this component using a reference to the CAS (here an {@link AnnotationDocument}) and let
 * the component fetch the associated CAS itself when necessary.
 *
 */
public class BratAnnotationDocumentVisualizer
    extends BratVisualizer
{
    private final static Logger LOG = LoggerFactory.getLogger(BratAnnotationDocumentVisualizer.class);

    private static final long serialVersionUID = -5898873898138122798L;

    private boolean dirty = true;

    private String docData = EMPTY_DOC;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @Resource(name = "annotationService")
    private static AnnotationService annotationService;

    public BratAnnotationDocumentVisualizer(String id, IModel<AnnotationDocument> aModel)
    {
        super(id, aModel);
    }

    public void setModel(IModel<AnnotationDocument> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(AnnotationDocument aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotationDocument> getModel()
    {
        return (IModel<AnnotationDocument>) getDefaultModel();
    }

    public AnnotationDocument getModelObject()
    {
        return (AnnotationDocument) getDefaultModelObject();
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        dirty = true;
    }

    @Override
    protected String getDocumentData()
    {
        if (!dirty) {
            return docData;
        }

        dirty = false;

        // Clear the rendered document
        docData = EMPTY_DOC;

        // Check if a document is set
        if (getModelObject() == null) {
            return docData;
        }

        // Get CAS from the repository
        JCas jCas = null;
        try {
            jCas = repository.readAnnotationCas(getModelObject());
        }
        catch (IOException | DataRetrievalFailureException e) {
            LOG.error("Unable to read annotation document", e);
            error("Unable to read annotation document: " + ExceptionUtils.getRootCauseMessage(e));
        }
        // Generate BRAT object model from CAS
        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        AnnotatorStateImpl bratAnnotatorModel = new AnnotatorStateImpl(Mode.ANNOTATION);
        BratRenderer.renderTokenAndSentence(jCas, response, bratAnnotatorModel);

        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : bratAnnotatorModel.getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())) {
                continue;
            }
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for (AnnotationFeature feature : features) {
                if (!feature.isVisible()) {
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);

            ColoringStrategy coloringStrategy = ColoringStrategy.getBestStrategy(annotationService,
                    layer, bratAnnotatorModel.getPreferences(), colorQueues);

            TypeAdapter typeAdapter = getAdapter(annotationService, layer);
            TypeRenderer typeRenderer = BratRenderer.getRenderer(typeAdapter);
            typeRenderer.render(jCas, features, response, bratAnnotatorModel, coloringStrategy);
        }

        // Serialize BRAT object model to JSON
        try {
            docData = JSONUtil.toInterpretableJsonString(response);
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }

        return docData;
    }
}
