/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.core.docanno.layer;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.ui.core.docanno.event.DocumentMetadataCreatedEvent;
import de.tudarmstadt.ukp.inception.ui.core.docanno.event.DocumentMetadataDeletedEvent;

public class DocumentMetadataLayerAdapter
    extends TypeAdapter_ImplBase
{
    public DocumentMetadataLayerAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Collection<AnnotationFeature> aFeatures)
    {
        super(aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);
    }

    @Override
    public long getTypeId()
    {
        return getLayer().getId();
    }

    @Override
    public Type getAnnotationType(CAS aCas)
    {
        return CasUtil.getType(aCas, getAnnotationTypeName());
    }

    @Override
    public String getAnnotationTypeName()
    {
        return getLayer().getName();
    }

    @Override
    public String getAttachFeatureName()
    {
        return null;
    }

    @Override
    public String getAttachTypeName()
    {
        return null;
    }

    /**
     * Add new document metadata annotation into the CAS and return the the id of the annotation.
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS.
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation cannot be created/updated.
     */
    public AnnotationBaseFS add(SourceDocument aDocument, String aUsername, CAS aCas)
        throws AnnotationException
    {
        Type type = CasUtil.getType(aCas, getAnnotationTypeName());
        
        AnnotationBaseFS newAnnotation = aCas.createFS(type);
        aCas.addFsToIndexes(newAnnotation);
        
        publishEvent(new DocumentMetadataCreatedEvent(this, aDocument, aUsername, newAnnotation));
        
        return newAnnotation;
    }
    
    @Override
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        AnnotationBase fs = selectByAddr(aCas, AnnotationBase.class, aVid.getId());
        aCas.removeFsFromIndexes(fs);

        publishEvent(new DocumentMetadataDeletedEvent(this, aDocument, aUsername, fs));
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        // There are no behaviors for document metadata annotations yet
        /*
        for (SpanLayerBehavior behavior : behaviors) {
            messages.addAll(behavior.onValidate(this, aJCas));
        }
        */
        return messages;
    }
}
