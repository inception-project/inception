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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.DeleteSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanMovedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;

/**
 * Manage interactions with annotations on a span layer.
 */
public class SpanAdapterImpl
    extends TypeAdapter_ImplBase
    implements SpanAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<SpanLayerBehavior> behaviors;

    private final SegmentationUnitAdapter segmentationUnitAdapter;

    public SpanAdapterImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures, List<SpanLayerBehavior> aBehaviors,
            ConstraintsService aConstraintsService)
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

        segmentationUnitAdapter = new SegmentationUnitAdapter(this);
    }

    @Override
    public AnnotationFS add(SourceDocument aDocument, String aDataOwner, CAS aCas, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        return handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(aDocument, aDataOwner, aCas) //
                .withRange(aBegin, aEnd) //
                .build());
    }

    @Override
    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (segmentationUnitAdapter.accepts(getAnnotationTypeName())) {
            return segmentationUnitAdapter.handle(aRequest);
        }

        var request = aRequest;

        // Adjust the creation request (e.g. adjust offsets to the configured granularity) or
        // reject the creation (e.g. reject cross-sentence annotations)
        for (var behavior : behaviors) {
            request = behavior.onCreate(this, request);
        }

        var newAnnotation = createSpanAnnotation(request.getCas(), request.getBegin(),
                request.getEnd());

        var finalRequest = request;
        publishEvent(() -> new SpanCreatedEvent(this, finalRequest.getDocument(),
                finalRequest.getDocumentOwner(), getLayer(), newAnnotation));

        return newAnnotation;
    }

    @Override
    public AnnotationFS handle(MoveSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (segmentationUnitAdapter.accepts(getAnnotationTypeName())) {
            return segmentationUnitAdapter.handle(aRequest);
        }

        var ann = aRequest.getAnnotation();
        if (aRequest.getBegin() == ann.getBegin() && aRequest.getEnd() == ann.getEnd()) {
            // NOP
            return ann;
        }

        var request = aRequest;

        // Adjust the move request (e.g. adjust offsets to the configured granularity) or
        // reject the request (e.g. reject cross-sentence annotations)
        for (var behavior : behaviors) {
            request = behavior.onMove(this, request);
        }

        var oldBegin = request.getAnnotation().getBegin();
        var oldEnd = request.getAnnotation().getEnd();
        moveSpanAnnotation(request.getCas(), request.getAnnotation(), request.getBegin(),
                request.getEnd());

        var finalRequest = request;
        publishEvent(() -> new SpanMovedEvent(this, finalRequest.getDocument(),
                finalRequest.getDocumentOwner(), getLayer(), finalRequest.getAnnotation(), oldBegin,
                oldEnd));

        return request.getAnnotation();
    }

    AnnotationFS createSpanAnnotation(CAS aCas, int aBegin, int aEnd, SpanOption... aOptions)
        throws AnnotationException
    {
        var type = CasUtil.getType(aCas, getAnnotationTypeName());
        var newAnnotation = (Annotation) aCas.createAnnotation(type, aBegin, aEnd);

        if (asList(aOptions).contains(SpanOption.TRIM)) {
            TrimUtils.trim(aCas.getDocumentText(), newAnnotation);
        }

        for (var feature : listFeatures()) {
            var maybeSupport = getFeatureSupport(feature.getName());
            if (maybeSupport.isPresent()) {
                maybeSupport.get().initializeAnnotation(feature, newAnnotation);
            }
        }

        LOG.trace("Created span annotation {}-{} [{}]", newAnnotation.getBegin(),
                newAnnotation.getEnd(), newAnnotation.getCoveredText());

        // If if the layer attaches to a feature, then set the attach-feature to the newly
        // created annotation.
        if (getAttachFeatureName() != null) {
            attach(aCas, aBegin, aEnd, newAnnotation);
        }

        aCas.addFsToIndexes(newAnnotation);

        return newAnnotation;
    }

    AnnotationFS moveSpanAnnotation(CAS aCas, AnnotationFS aAnnotation, int aBegin, int aEnd,
            SpanOption... aOptions)
    {
        var oldCoveredText = aAnnotation.getCoveredText();
        var oldBegin = aAnnotation.getBegin();
        var oldEnd = aAnnotation.getEnd();

        aCas.removeFsFromIndexes(aAnnotation);
        aAnnotation.setBegin(aBegin);
        aAnnotation.setEnd(aEnd);

        if (asList(aOptions).contains(SpanOption.TRIM)) {
            TrimUtils.trim(aCas.getDocumentText(), (Annotation) aAnnotation);
        }

        LOG.trace("Moved span annotation from {}-{} [{}] to {}-{} [{}]", oldBegin, oldEnd,
                oldCoveredText, aAnnotation.getBegin(), aAnnotation.getEnd(),
                aAnnotation.getCoveredText());

        aCas.addFsToIndexes(aAnnotation);

        return aAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aDocumentOwner, CAS aCas, VID aVid)
        throws AnnotationException
    {
        var fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        handle(new DeleteSpanAnnotationRequest(aDocument, aDocumentOwner, aCas, fs));
    }

    @Override
    public void handle(DeleteSpanAnnotationRequest aRequest) throws AnnotationException
    {
        if (segmentationUnitAdapter.accepts(getAnnotationTypeName())) {
            segmentationUnitAdapter.handle(aRequest);
            return;
        }

        aRequest.getCas().removeFsFromIndexes(aRequest.getAnnotation());

        // delete associated attachFeature
        if (getAttachTypeName() != null) {
            detach(aRequest.getCas(), aRequest.getAnnotation());
        }

        publishEvent(() -> new SpanDeletedEvent(this, aRequest.getDocument(),
                aRequest.getDocumentOwner(), getLayer(), aRequest.getAnnotation()));
    }

    @Override
    public AnnotationFS restore(SourceDocument aDocument, String aDocumentOwner, CAS aCas, VID aVid)
        throws AnnotationException
    {
        var fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());

        if (getAttachFeatureName() != null) {
            attach(aCas, fs.getBegin(), fs.getEnd(), fs);
        }

        aCas.addFsToIndexes(fs);

        publishEvent(() -> new SpanCreatedEvent(this, aDocument, aDocumentOwner, getLayer(), fs));

        return fs;
    }

    private void attach(CAS aCas, int aBegin, int aEnd, AnnotationFS newAnnotation)
        throws IllegalPlacementException
    {
        var theType = getType(aCas, getAttachTypeName());
        var attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
        if (selectCovered(aCas, theType, aBegin, aEnd).isEmpty()) {
            throw new IllegalPlacementException("No annotation of type [" + getAttachTypeName()
                    + "] to attach to at location [" + aBegin + "-" + aEnd + "].");
        }
        selectCovered(aCas, theType, aBegin, aEnd).get(0).setFeatureValue(attachFeature,
                newAnnotation);
    }

    private void detach(CAS aCas, AnnotationFS fs)
    {
        var theType = getType(aCas, getAttachTypeName());
        var attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
        if (attachFeature != null) {
            selectCovered(aCas, theType, fs.getBegin(), fs.getEnd()).get(0)
                    .setFeatureValue(attachFeature, null);
        }
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        var messages = new ArrayList<Pair<LogMessage, AnnotationFS>>();

        for (var behavior : behaviors) {
            var startTime = currentTimeMillis();
            messages.addAll(behavior.onValidate(this, aCas));
            LOG.trace("Validation for [{}] on [{}] took {}ms", behavior.getClass().getSimpleName(),
                    getLayer().getUiName(), currentTimeMillis() - startTime);
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

        if (aFS1 instanceof AnnotationFS ann1 && aFS2 instanceof AnnotationFS ann2) {
            if (aFS1 == aFS2) {
                return true;
            }

            return colocated(ann1, ann2);
        }

        throw new IllegalArgumentException("Feature structures need to be annotations");
    }
}
