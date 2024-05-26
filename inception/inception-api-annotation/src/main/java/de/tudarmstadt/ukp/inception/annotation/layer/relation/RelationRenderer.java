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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 */
public class RelationRenderer
    extends Renderer_ImplBase<RelationAdapter>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final VID VID_BEFORE = VID.builder() //
            .withExtensionId("rel") //
            .withAnnotationId(0) //
            .withExtensionPayload("before") //
            .build();

    private static final VID VID_AFTER = VID.builder() //
            .withExtensionId("rel") //
            .withAnnotationId(1) //
            .withExtensionPayload("after") //
            .build();

    private final List<RelationLayerBehavior> behaviors;
    private final RelationLayerTraits traits;

    private Type type;
    private Type spanType;
    private Feature targetFeature;
    private Feature sourceFeature;
    private Feature attachFeature;

    public RelationRenderer(RelationAdapter aTypeAdapter,
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, List<RelationLayerBehavior> aBehaviors)
    {
        super(aTypeAdapter, aLayerSupportRegistry, aFeatureSupportRegistry);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            var temp = new ArrayList<RelationLayerBehavior>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }

        var typeAdapter = getTypeAdapter();
        traits = typeAdapter.getTraits(RelationLayerTraits.class)
                .orElseGet(RelationLayerTraits::new);
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        var typeAdapter = getTypeAdapter();
        type = aTypeSystem.getType(typeAdapter.getAnnotationTypeName());
        spanType = aTypeSystem.getType(typeAdapter.getAttachTypeName());

        if (type == null || spanType == null) {
            // If the types are not defined, then we do not need to try and render them because the
            // CAS does not contain any instances of them
            return false;
        }

        targetFeature = type.getFeatureByBaseName(typeAdapter.getTargetFeatureName());
        sourceFeature = type.getFeatureByBaseName(typeAdapter.getSourceFeatureName());
        attachFeature = spanType.getFeatureByBaseName(typeAdapter.getAttachFeatureName());

        return true;
    }

    @Override
    public List<AnnotationFS> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        var result = new ArrayList<AnnotationFS>();

        for (var rel : aCas.<Annotation> select(type)) {
            var sourceFs = getSourceFs(rel);
            var targetFs = getTargetFs(rel);

            if (sourceFs instanceof Annotation source && targetFs instanceof Annotation target) {
                var relBegin = Math.min(source.getBegin(), target.getBegin());
                var relEnd = Math.max(source.getEnd(), target.getEnd());

                if (overlapping(relBegin, relEnd, aWindowBegin, aWindowEnd)) {
                    result.add(rel);
                }
            }
        }

        return result;
    }

    @Override
    public void render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse, int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aRequest.getCas())) {
            return;
        }

        var typeAdapter = getTypeAdapter();

        // Index mapping annotations to the corresponding rendered arcs
        var annoToArcIdx = new HashMap<AnnotationFS, VArc>();

        var annotations = selectAnnotationsInWindow(aRequest.getCas(), aWindowBegin, aWindowEnd);

        for (var fs : annotations) {
            for (var obj : render(aRequest, aFeatures, aResponse, aWindowBegin, aWindowEnd, fs)) {
                if (!(obj instanceof VArc)) {
                    aResponse.add(obj);
                    continue;
                }

                aResponse.add(obj);
                annoToArcIdx.put(fs, (VArc) obj);

                renderRequiredFeatureErrors(aRequest, aFeatures, fs, aResponse);
            }
        }

        for (var behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToArcIdx);
        }
    }

    Optional<String> renderYield(AnnotationFS fs)
    {
        var yield = new HashSet<Annotation>();
        var queue = new ArrayDeque<Annotation>();
        queue.add((Annotation) getTargetFs(fs));
        var relationsBySource = fs.getCAS().<Annotation> select(type)
                .collect(groupingBy(this::getSourceFs));
        while (!queue.isEmpty()) {
            var source = queue.pop();
            if (!yield.contains(source)) {
                yield.add(source);
                var relations = relationsBySource.getOrDefault(source, emptyList());
                for (var rel : relations) {
                    queue.add((Annotation) getTargetFs(rel));
                }
            }
        }

        var sortedYield = yield.stream() //
                .sorted(Comparator.comparingInt(Annotation::getBegin)) //
                .toList();
        var message = getYieldMessage(sortedYield);
        return Optional.of(message);
    }

    @Override
    public List<VObject> render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aVDocument, int aWindowBegin, int aWindowEnd, AnnotationFS aFS)
    {
        if (!checkTypeSystem(aFS.getCAS())) {
            return emptyList();
        }

        var typeAdapter = getTypeAdapter();
        var sourceFs = getSourceFs(aFS);
        var targetFs = getTargetFs(aFS);

        if (targetFs == null || sourceFs == null) {
            WicketUtil.getPage().ifPresent(page -> {
                var message = new StringBuilder();
                message.append("Relation [" + typeAdapter.getLayer().getName() + "] with id ["
                        + ICasUtil.getAddr(aFS) + "] has loose ends - cannot render.");
                if (typeAdapter.getAttachFeatureName() != null) {
                    message.append("\nRelation [" + typeAdapter.getLayer().getName()
                            + "] attached to feature [" + typeAdapter.getAttachFeatureName()
                            + "].");
                }
                message.append("\nSource: " + sourceFs);
                message.append("\nTarget: " + targetFs);
                page.warn(message.toString());
            });

            return emptyList();
        }

        var labelFeatures = renderLabelFeatureValues(typeAdapter, aFS, aFeatures);

        switch (traits.getRenderMode()) {
        case ALWAYS:
            return renderRelationAsArcs(aRequest, aVDocument, aFS, typeAdapter, sourceFs, targetFs,
                    labelFeatures, aWindowBegin, aWindowEnd);
        case WHEN_SELECTED:
            if (aRequest.getState() == null || isSelected(aRequest, aFS, sourceFs, targetFs)) {
                return renderRelationAsArcs(aRequest, aVDocument, aFS, typeAdapter, sourceFs,
                        targetFs, labelFeatures, aWindowBegin, aWindowEnd);
            }
            return renderRelationOnLabel(aVDocument, typeAdapter, sourceFs, targetFs,
                    labelFeatures);
        case NEVER:
            return renderRelationOnLabel(aVDocument, typeAdapter, sourceFs, targetFs,
                    labelFeatures);
        default:
            return renderRelationAsArcs(aRequest, aVDocument, aFS, typeAdapter, sourceFs, targetFs,
                    labelFeatures, aWindowBegin, aWindowEnd);
        }
    }

    private boolean isSelected(RenderRequest aRequest, FeatureStructure... aFSes)
    {
        var selection = aRequest.getState().getSelection();

        if (!selection.isSet()) {
            return false;
        }

        for (var fs : aFSes) {
            if (VID.of(fs).equals(selection.getAnnotation())) {
                return true;
            }
        }

        return false;
    }

    private List<VObject> renderRelationOnLabel(VDocument aVDocument, RelationAdapter typeAdapter,
            FeatureStructure sourceFs, FeatureStructure targetFs, Map<String, String> labelFeatures)
    {
        var source = (AnnotationFS) sourceFs;
        var target = (AnnotationFS) targetFs;

        var noteBuilder = new StringBuilder();
        noteBuilder.append(typeAdapter.getLayer().getUiName());
        noteBuilder.append("\n");
        noteBuilder.append(source.getCoveredText());
        noteBuilder.append(" -> ");
        noteBuilder.append(target.getCoveredText());
        noteBuilder.append("\n");

        for (var entry : labelFeatures.entrySet()) {
            noteBuilder.append(entry.getKey());
            if (StringUtils.isNotBlank(entry.getValue())) {
                noteBuilder.append(" = ");
                noteBuilder.append(entry.getValue());
            }
            noteBuilder.append("\n");
        }

        var note = noteBuilder.toString().stripTrailing();
        aVDocument.add(new VComment(sourceFs, VCommentType.INFO, "\n⏺➡" + note));
        aVDocument.add(new VComment(targetFs, VCommentType.INFO, "\n➡⏺" + note));

        return emptyList();
    }

    private List<VObject> renderRelationAsArcs(RenderRequest aRequest, VDocument aVDocument,
            AnnotationFS aFS, RelationAdapter typeAdapter, FeatureStructure sourceFs,
            FeatureStructure targetFs, Map<String, String> labelFeatures, int aWindowBegin,
            int aWindowEnd)
    {
        var objects = new ArrayList<VObject>();

        var source = createRelationEndpoint(aRequest, aVDocument, (AnnotationFS) sourceFs,
                aWindowBegin, aWindowEnd, typeAdapter);

        var target = createRelationEndpoint(aRequest, aVDocument, (AnnotationFS) targetFs,
                aWindowBegin, aWindowEnd, typeAdapter);

        objects.add(VArc.builder().forAnnotation(aFS) //
                .withLayer(typeAdapter.getLayer()) //
                .withSource(source) //
                .withTarget(target) //
                .withFeatures(labelFeatures) //
                .build());

        return objects;
    }

    private VID createRelationEndpoint(RenderRequest aRequest, VDocument aVDocument,
            AnnotationFS aEndpoint, int aWindowBegin, int aWindowEnd, RelationAdapter aTypeAdapter)
    {
        if (aEndpoint.getEnd() < aWindowBegin) {
            if (aRequest.isClipRelations()) {
                if (aVDocument.getSpan(VID_BEFORE) == null) {
                    var beforeAnchor = VSpan.builder() //
                            .withVid(VID_BEFORE) //
                            .withLayer(aTypeAdapter.getLayer()) //
                            .withRange(new VRange(0, 0)) //
                            .build();
                    aVDocument.add(beforeAnchor);
                }
                return VID_BEFORE;
            }

            var placeholder = VSpan.builder() //
                    .forAnnotation(aEndpoint) //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(aEndpoint.getBegin() - aWindowBegin,
                            aEndpoint.getEnd() - aWindowBegin)) //
                    .build();
            aVDocument.add(placeholder);
            return placeholder.getVid();
        }

        if (aEndpoint.getBegin() >= aWindowEnd) {
            if (aRequest.isClipRelations()) {
                if (aVDocument.getSpan(VID_AFTER) == null) {
                    var afterAnchor = VSpan.builder() //
                            .withVid(VID_AFTER) //
                            .withLayer(aTypeAdapter.getLayer()) //
                            .withRange(new VRange(aWindowEnd - aWindowBegin,
                                    aWindowEnd - aWindowBegin)) //
                            .build();
                    aVDocument.add(afterAnchor);
                }
                return VID_AFTER;
            }

            var placeholder = VSpan.builder() //
                    .forAnnotation(aEndpoint) //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(aEndpoint.getBegin() - aWindowBegin,
                            aEndpoint.getEnd() - aWindowBegin)) //
                    .build();
            aVDocument.add(placeholder);
            return placeholder.getVid();
        }

        return VID.of(aEndpoint);
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(CAS aCas, VID aVid)
    {
        if (!checkTypeSystem(aCas)) {
            return emptyList();
        }

        var fs = ICasUtil.selectAnnotationByAddr(aCas, aVid.getId());

        var group = new VLazyDetailGroup();

        var targetFs = getTargetFs(fs);
        if (targetFs instanceof AnnotationFS) {
            group.addDetail(new VLazyDetail("Target",
                    abbreviate(((AnnotationFS) targetFs).getCoveredText(), 300)));
        }

        var sourceFs = getSourceFs(fs);
        if (sourceFs instanceof AnnotationFS) {
            group.addDetail(new VLazyDetail("Source",
                    abbreviate(((AnnotationFS) sourceFs).getCoveredText(), 300)));
        }

        renderYield(fs).ifPresent(
                yield -> group.addDetail(new VLazyDetail("Yield", abbreviate(yield, "...", 300))));

        var details = super.lookupLazyDetails(aCas, aVid);
        if (!group.getDetails().isEmpty()) {
            details.add(0, group);
        }
        return details;
    }

    /**
     * The relations yield message
     */
    private String getYieldMessage(Iterable<Annotation> sortedDepFs)
    {
        var cm = new StringBuilder();
        int end = -1;
        for (var depFs : sortedDepFs) {
            if (end == -1) {
                cm.append(depFs.getCoveredText());
                end = depFs.getEnd();
            }
            // if no space between token and punct
            else if (end == depFs.getBegin()) {
                cm.append(depFs.getCoveredText());
                end = depFs.getEnd();
            }
            else if (end + 1 != depFs.getBegin()) {
                cm.append(" ... ").append(depFs.getCoveredText());
                end = depFs.getEnd();
            }
            else {
                cm.append(" ").append(depFs.getCoveredText());
                end = depFs.getEnd();
            }

        }
        return cm.toString();
    }

    private FeatureStructure getSourceFs(FeatureStructure fs)
    {
        if (attachFeature != null) {
            return fs.getFeatureValue(sourceFeature).getFeatureValue(attachFeature);
        }

        return fs.getFeatureValue(sourceFeature);
    }

    private FeatureStructure getTargetFs(FeatureStructure fs)
    {
        if (attachFeature != null) {
            return fs.getFeatureValue(targetFeature).getFeatureValue(attachFeature);
        }

        return fs.getFeatureValue(targetFeature);
    }
}
