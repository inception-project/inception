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
package de.tudarmstadt.ukp.inception.annotation.layer.document;

import static de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport.FEATURE_NAME_ORDER;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.CreateDocumentAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.event.DocumentMetadataCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.event.DocumentMetadataDeletedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class DocumentMetadataLayerAdapterImpl
    extends TypeAdapter_ImplBase
    implements DocumentMetadataLayerAdapter
{
    private final List<DocumentMetadataLayerBehavior> behaviors;

    public DocumentMetadataLayerAdapterImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures,
            ConstraintsService aConstraintsService, List<DocumentMetadataLayerBehavior> aBehaviors)
    {
        super(aLayerSupportRegistry, aFeatureSupportRegistry, aConstraintsService, aEventPublisher,
                aLayer, aFeatures);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            behaviors = aBehaviors.stream() //
                    .sorted(AnnotationAwareOrderComparator.INSTANCE) //
                    .toList();
        }
    }

    @Override
    public long getTypeId()
    {
        return getLayer().getId();
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

    @Override
    public AnnotationBase add(SourceDocument aDocument, String aUsername, CAS aCas)
        throws AnnotationException
    {
        var type = getType(aCas, getAnnotationTypeName());

        AnnotationBase newAnnotation = aCas.createFS(type);
        var maxOrder = aCas.select(type) //
                .mapToInt(fs -> FSUtil.getFeature(fs, FEATURE_NAME_ORDER, Integer.class)) //
                .max() //
                .orElse(0);
        setFeature(newAnnotation, FEATURE_NAME_ORDER, (int) maxOrder + 1);
        aCas.addFsToIndexes(newAnnotation);

        publishEvent(() -> new DocumentMetadataCreatedEvent(this, aDocument, aUsername, getLayer(),
                newAnnotation));

        return newAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        var fs = (AnnotationBaseFS) selectFsByAddr(aCas, aVid.getId());
        aCas.removeFsFromIndexes(fs);

        publishEvent(
                () -> new DocumentMetadataDeletedEvent(this, aDocument, aUsername, getLayer(), fs));
    }

    @Override
    public AnnotationBaseFS restore(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        var fs = (AnnotationBaseFS) selectFsByAddr(aCas, aVid.getId());

        aCas.addFsToIndexes(fs);

        publishEvent(
                () -> new DocumentMetadataCreatedEvent(this, aDocument, aUsername, getLayer(), fs));
        return fs;
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        var messages = new ArrayList<Pair<LogMessage, AnnotationFS>>();

        for (var behavior : behaviors) {
            messages.addAll(behavior.onValidate(this, aCas));
        }

        return messages;
    }

    @Override
    public Selection select(VID aVid, AnnotationFS aAnno)
    {
        var selection = new Selection();
        selection.selectSpan(aAnno);
        return selection;
    }

    @Override
    public boolean isSamePosition(FeatureStructure aFS1, FeatureStructure aFS2)
    {
        if (aFS1 == null || aFS2 == null) {
            return false;
        }

        if (!aFS1.getType().getName().equals(getAnnotationTypeName())) {
            throw new IllegalArgumentException("Expected [" + getAnnotationTypeName()
                    + "] but got [" + aFS1.getType().getName() + "]");
        }

        if (!aFS2.getType().getName().equals(getAnnotationTypeName())) {
            throw new IllegalArgumentException("Expected [" + getAnnotationTypeName()
                    + "] but got [" + aFS2.getType().getName() + "]");
        }

        if (aFS1 instanceof AnnotationBaseFS && aFS2 instanceof AnnotationBaseFS) {
            return true;
            // if (aFS1 == aFS2) {
            // return true;
            // }
            //
            // return Objects.equals(ann1.getView().getViewName(), ann2.getView().getViewName());
        }

        throw new IllegalArgumentException("Feature structures need to be AnnotationBaseFS");
    }

    @Override
    public AnnotationBase handle(CreateDocumentAnnotationRequest aCreateDocumentAnnotationRequest)
        throws AnnotationException
    {
        return add(aCreateDocumentAnnotationRequest.getDocument(),
                aCreateDocumentAnnotationRequest.getDocumentOwner(),
                aCreateDocumentAnnotationRequest.getCas());
    }
}
