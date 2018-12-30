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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A class that is used to create Brat Span to CAS and vice-versa.
 */
public class SpanAdapter
    extends TypeAdapter_ImplBase
{
    public SpanAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Collection<AnnotationFeature> aFeatures)
    {
        super(aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);
    }

    /**
     * Add new span annotation into the CAS and return the the id of the span annotation
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aJCas
     *            the JCas.
     * @param aBegin
     *            the begin offset.
     * @param aEnd
     *            the end offset.
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation cannot be created/updated.
     */
    public AnnotationFS add(SourceDocument aDocument, String aUsername, JCas aJCas, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        return handle(new CreateSpanAnnotationRequest(aDocument, aUsername, aJCas, aBegin, aEnd));
    }
    
    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        CreateSpanAnnotationRequest request = aRequest;

        request = new SpanCrossSentenceBehavior().apply(this, request);

        request = new SpanAnchoringModeBehavior().apply(this, request);

        request = new SpanStackingBehavior().apply(this, request);

        AnnotationFS newAnnotation = createSpanAnnotation(request.getJcas().getCas(),
                request.getBegin(), request.getEnd());

        publishEvent(new SpanCreatedEvent(this, request.getDocument(), request.getUsername(),
                newAnnotation));

        return newAnnotation;
    }
    
    /**
     * A Helper method to add annotation to CAS
     */
    private AnnotationFS createSpanAnnotation(CAS aCas, int aBegin, int aEnd)
        throws AnnotationException
    {
        Type type = CasUtil.getType(aCas, getAnnotationTypeName());
        AnnotationFS newAnnotation = aCas.createAnnotation(type, aBegin, aEnd);
        
        // If if the layer attaches to a feature, then set the attach-feature to the newly
        // created annotation.
        if (getAttachFeatureName() != null) {
            Type theType = CasUtil.getType(aCas, getAttachTypeName());
            Feature attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
            if (CasUtil.selectCovered(aCas, theType, aBegin, aEnd).isEmpty()) {
                throw new AnnotationException("No annotation of type [" + getAttachTypeName()
                        + "] to attach to at location [" + aBegin + "-" + aEnd + "].");
            }
            CasUtil.selectCovered(aCas, theType, aBegin, aEnd).get(0)
                    .setFeatureValue(attachFeature, newAnnotation);
        }
        
        aCas.addFsToIndexes(newAnnotation);
        
        return newAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, JCas aJCas, VID aVid)
    {
        AnnotationFS fs = selectByAddr(aJCas, AnnotationFS.class, aVid.getId());
        aJCas.removeFsFromIndexes(fs);

        // delete associated attachFeature
        if (getAttachTypeName() != null) {
            Type theType = CasUtil.getType(aJCas.getCas(), getAttachTypeName());
            Feature attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
            if (attachFeature != null) {
                CasUtil.selectCovered(aJCas.getCas(), theType, fs.getBegin(), fs.getEnd()).get(0)
                        .setFeatureValue(attachFeature, null);
            }
        }
        
        publishEvent(new SpanDeletedEvent(this, aDocument, aUsername, getLayer(), fs));
    }

    /**
     * The UIMA type name.
     */
    @Override
    public String getAnnotationTypeName()
    {
        return getLayer().getName();
    }
}
