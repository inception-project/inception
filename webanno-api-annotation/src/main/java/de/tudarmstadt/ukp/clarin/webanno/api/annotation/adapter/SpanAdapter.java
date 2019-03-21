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
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Manage interactions with annotations on a span layer.
 */
public class SpanAdapter
    extends TypeAdapter_ImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<SpanLayerBehavior> behaviors;
    
    public SpanAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Collection<AnnotationFeature> aFeatures, List<SpanLayerBehavior> aBehaviors)
    {
        super(aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);
        
        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            List<SpanLayerBehavior> temp = new ArrayList<>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }
    }

    /**
     * Add new span annotation into the CAS and return the the id of the span annotation
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS.
     * @param aBegin
     *            the begin offset.
     * @param aEnd
     *            the end offset.
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation cannot be created/updated.
     */
    public AnnotationFS add(SourceDocument aDocument, String aUsername, CAS aCas, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        return handle(new CreateSpanAnnotationRequest(aDocument, aUsername, aCas, aBegin, aEnd));
    }
    
    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        CreateSpanAnnotationRequest request = aRequest;

        for (SpanLayerBehavior behavior : behaviors) {
            request = behavior.onCreate(this, request);
        }

        AnnotationFS newAnnotation = createSpanAnnotation(request.getCas(), request.getBegin(),
                request.getEnd());

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
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        AnnotationFS fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        aCas.removeFsFromIndexes(fs);

        // delete associated attachFeature
        if (getAttachTypeName() != null) {
            Type theType = CasUtil.getType(aCas, getAttachTypeName());
            Feature attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
            if (attachFeature != null) {
                CasUtil.selectCovered(aCas, theType, fs.getBegin(), fs.getEnd()).get(0)
                        .setFeatureValue(attachFeature, null);
            }
        }
        
        publishEvent(new SpanDeletedEvent(this, aDocument, aUsername, getLayer(), fs));
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        for (SpanLayerBehavior behavior : behaviors) {
            long startTime = currentTimeMillis();
            messages.addAll(behavior.onValidate(this, aCas));
            log.trace("Validation for [{}] on [{}] took {}ms", behavior.getClass().getSimpleName(),
                    getLayer().getUiName(), currentTimeMillis() - startTime);
        }
        return messages;
    }
}
