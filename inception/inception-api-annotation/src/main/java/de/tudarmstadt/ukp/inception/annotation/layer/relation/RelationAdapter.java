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

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEndpointFeatureSupport.PREFIX_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEndpointFeatureSupport.PREFIX_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectByAddr;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
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
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.FeatureFilter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * Manage interactions with annotations on a relation layer.
 */
public class RelationAdapter
    extends TypeAdapter_ImplBase
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

    public RelationAdapter(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            String aTargetFeatureName, String aSourceFeatureName,
            Supplier<Collection<AnnotationFeature>> aFeatures,
            List<RelationLayerBehavior> aBehaviors, ConstraintsService aConstraintsService)
    {
        super(aLayerSupportRegistry, aFeatureSupportRegistry, aConstraintsService, aEventPublisher, aLayer,
                aFeatures);

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

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aOriginFs
     *            the origin FS.
     * @param aTargetFs
     *            the target FS.
     * @param aCas
     *            the CAS.
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation could not be created/updated.
     */
    public AnnotationFS add(SourceDocument aDocument, String aUsername, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, CAS aCas)
        throws AnnotationException
    {
        return handle(new CreateRelationAnnotationRequest(aDocument, aUsername, aCas, aOriginFs,
                aTargetFs));
    }

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

    public AnnotationFS restore(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
        throws AnnotationException
    {
        var fs = selectByAddr(aCas, AnnotationFS.class, aVid.getId());
        aCas.addFsToIndexes(fs);

        publishEvent(() -> new RelationCreatedEvent(this, aDocument, aUsername, getLayer(), fs,
                getTargetAnnotation(fs), getSourceAnnotation(fs)));

        return fs;
    }

    public AnnotationFS getSourceAnnotation(AnnotationFS aTargetFs)
    {
        var sourceFeature = aTargetFs.getType().getFeatureByBaseName(sourceFeatureName);
        if (sourceFeature == null) {
            return null;
        }
        return (AnnotationFS) aTargetFs.getFeatureValue(sourceFeature);
    }

    public AnnotationFS getTargetAnnotation(AnnotationFS aTargetFs)
    {
        var targetFeature = aTargetFs.getType().getFeatureByBaseName(targetFeatureName);
        if (targetFeature == null) {
            return null;
        }
        return (AnnotationFS) aTargetFs.getFeatureValue(targetFeature);
    }

    public String getSourceFeatureName()
    {
        return sourceFeatureName;
    }

    public Feature getSourceFeature(CAS aCas)
    {
        return getAnnotationType(aCas).getFeatureByBaseName(getSourceFeatureName());
    }

    public String getTargetFeatureName()
    {
        return targetFeatureName;
    }

    public Feature getTargetFeature(CAS aCas)
    {
        return getAnnotationType(aCas).getFeatureByBaseName(getTargetFeatureName());
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
    public boolean equivalents(AnnotationFS aFs1, AnnotationFS aFs2, FeatureFilter aFilter)
    {
        if (!super.equivalents(aFs1, aFs2, aFilter)) {
            return false;
        }

        // So if the basic span-oriented comparison returned true, we still must ensure that the
        // relation endpoints are also equivalent. Here, we only consider the endpoint type and
        // position but not any other features.
        var fs1Source = getSourceAnnotation(aFs1);
        var fs1Target = getTargetAnnotation(aFs1);
        var fs2Source = getSourceAnnotation(aFs2);
        var fs2Target = getTargetAnnotation(aFs2);

        return sameBeginEndAndType(fs1Source, fs2Source)
                && sameBeginEndAndType(fs1Target, fs2Target);
    }

    private boolean sameBeginEndAndType(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        return aFs1.getBegin() == aFs2.getBegin() && //
                aFs1.getEnd() == aFs2.getEnd() && //
                Objects.equals(aFs1.getType().getName(), aFs2.getType().getName());
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
