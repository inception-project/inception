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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.PREFIX_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.PREFIX_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.hasSameType;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
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
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * Manage interactions with annotations on a relation layer.
 */
public class RelationAdapterImpl
    extends TypeAdapter_ImplBase
    implements RelationAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The feature of an UIMA annotation containing the label to be used as a governor for arc
     * annotations
     */
    private final String sourceFeatureName;

    /**
     * The feature of an UIMA annotation containing the label to be used as a dependent for arc
     * annotations
     */
    private final String targetFeatureName;

    private final List<RelationLayerBehavior> behaviors;

    public RelationAdapterImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            String aTargetFeatureName, String aSourceFeatureName,
            Supplier<Collection<AnnotationFeature>> aFeatures,
            List<RelationLayerBehavior> aBehaviors, ConstraintsService aConstraintsService)
    {
        super(aLayerSupportRegistry, aFeatureSupportRegistry, aConstraintsService, aEventPublisher,
                aLayer, aFeatures);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            var temp = new ArrayList<RelationLayerBehavior>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }

        sourceFeatureName = aSourceFeatureName;
        targetFeatureName = aTargetFeatureName;
    }

    @Override
    public AnnotationFS add(SourceDocument aDocument, String aUsername, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, CAS aCas)
        throws AnnotationException
    {
        return handle(new CreateRelationAnnotationRequest(aDocument, aUsername, aCas, aOriginFs,
                aTargetFs));
    }

    @Override
    public AnnotationFS handle(CreateRelationAnnotationRequest aRequest) throws AnnotationException
    {
        var request = aRequest;

        for (var behavior : behaviors) {
            request = behavior.onCreate(this, request);
        }

        var relationAnno = createRelationAnnotation(request.getCas(), request.getOriginFs(),
                request.getTargetFs());

        var finalRequest = request;
        publishEvent(() -> new RelationCreatedEvent(this, finalRequest.getDocument(),
                finalRequest.getUsername(), getLayer(), relationAnno,
                getTargetAnnotation(relationAnno), getSourceAnnotation(relationAnno)));

        return relationAnno;
    }

    private AnnotationFS createRelationAnnotation(CAS cas, AnnotationFS originFS,
            AnnotationFS targetFS)
        throws AnnotationException
    {
        if (targetFS == null || originFS == null) {
            throw new IllegalPlacementException("Relation must have a source and a target!");
        }

        // Set the relation offsets in DKPro Core style - the relation receives the offsets from
        // the dependent
        // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
        // the begin position of the first token and dependentFS.getEnd will be the End
        // position of the last token.
        final var type = getType(cas, getLayer().getName());
        final var dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        final var governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        var newAnnotation = cas.createAnnotation(type, targetFS.getBegin(), targetFS.getEnd());
        newAnnotation.setFeatureValue(dependentFeature, targetFS);
        newAnnotation.setFeatureValue(governorFeature, originFS);
        cas.addFsToIndexes(newAnnotation);
        return newAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        var fs = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        aCas.removeFsFromIndexes(fs);
        publishEvent(() -> new RelationDeletedEvent(this, aDocument, aUsername, getLayer(), fs,
                getTargetAnnotation(fs), getSourceAnnotation(fs)));
    }

    @Override
    public AnnotationFS restore(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
        throws AnnotationException
    {
        var fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        aCas.addFsToIndexes(fs);

        publishEvent(() -> new RelationCreatedEvent(this, aDocument, aUsername, getLayer(), fs,
                getTargetAnnotation(fs), getSourceAnnotation(fs)));

        return fs;
    }

    @Override
    public AnnotationFS getSourceAnnotation(AnnotationFS aRelationFS)
    {
        var sourceFeature = aRelationFS.getType().getFeatureByBaseName(sourceFeatureName);
        if (sourceFeature == null) {
            return null;
        }
        return (AnnotationFS) aRelationFS.getFeatureValue(sourceFeature);
    }

    @Override
    public AnnotationFS getTargetAnnotation(AnnotationFS aRelationFS)
    {
        var targetFeature = aRelationFS.getType().getFeatureByBaseName(targetFeatureName);
        if (targetFeature == null) {
            return null;
        }
        return (AnnotationFS) aRelationFS.getFeatureValue(targetFeature);
    }

    @Override
    public String getSourceFeatureName()
    {
        return sourceFeatureName;
    }

    @Override
    public Feature getSourceFeature(CAS aCas)
    {
        return getAnnotationType(aCas) //
                .map(type -> type.getFeatureByBaseName(getSourceFeatureName())) //
                .orElse(null);
    }

    @Override
    public String getTargetFeatureName()
    {
        return targetFeatureName;
    }

    @Override
    public Feature getTargetFeature(CAS aCas)
    {
        return getAnnotationType(aCas) //
                .map(type -> type.getFeatureByBaseName(getTargetFeatureName())) //
                .orElse(null);
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        var messages = new ArrayList<Pair<LogMessage, AnnotationFS>>();
        for (var behavior : behaviors) {
            long startTime = currentTimeMillis();
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
        var src = getSourceAnnotation(aAnno);
        var tgt = getTargetAnnotation(aAnno);

        if (getLayer().getAttachFeature() != null) {
            src = FSUtil.getFeature(src, getLayer().getAttachFeature().getName(),
                    AnnotationFS.class);
            tgt = FSUtil.getFeature(tgt, getLayer().getAttachFeature().getName(),
                    AnnotationFS.class);
        }

        selection.selectArc(VID.of(aAnno), src, tgt);
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

            // So if the basic span-oriented comparison returned true, we still must ensure that the
            // relation endpoints are also equivalent. Here, we only consider the endpoint type and
            // position but not any other features.
            var fs1Source = getSourceAnnotation(ann1);
            var fs1Target = getTargetAnnotation(ann1);
            var fs2Source = getSourceAnnotation(ann2);
            var fs2Target = getTargetAnnotation(ann2);

            return isSameEndpoint(fs1Source, fs2Source) && isSameEndpoint(fs1Target, fs2Target);
        }

        throw new IllegalArgumentException("Feature structures need to be annotations");
    }

    private boolean isSameEndpoint(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        return hasSameType(aFs1, aFs2) && colocated(aFs1, aFs2);
    }

    @Override
    public void initializeLayerConfiguration(AnnotationSchemaService aSchemaService)
    {
        var sourceFeature = AnnotationFeature.builder() //
                .withLayer(getLayer()) //
                .withType(PREFIX_SOURCE + getAttachTypeName()) //
                .withName(getSourceFeatureName()) //
                .withUiName("Source") //
                .withEnabled(true) //
                .build();

        aSchemaService.createFeature(sourceFeature);

        var targetFeature = AnnotationFeature.builder() //
                .withLayer(getLayer()) //
                .withType(PREFIX_TARGET + getAttachTypeName()) //
                .withName(getTargetFeatureName()) //
                .withUiName("Target") //
                .withEnabled(true) //
                .build();

        aSchemaService.createFeature(targetFeature);
    }
}
