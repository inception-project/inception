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
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

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
                if (source.overlapping(aWindowBegin, aWindowEnd)
                        || target.overlapping(aWindowBegin, aWindowEnd)) {
                    result.add(rel);
                }
            }
        }
        return result;

        // return selectCovered(aCas, type, aWindowBegin, aWindowEnd);
    }

    @Override
    public void render(final CAS aCas, List<AnnotationFeature> aFeatures, VDocument aResponse,
            int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aCas)) {
            return;
        }

        var typeAdapter = getTypeAdapter();

        // Index mapping annotations to the corresponding rendered arcs
        var annoToArcIdx = new HashMap<AnnotationFS, VArc>();

        var annotations = selectAnnotationsInWindow(aCas, aWindowBegin, aWindowEnd);

        for (var fs : annotations) {
            for (var obj : render(aResponse, fs, aFeatures, aWindowBegin, aWindowEnd)) {
                if (!(obj instanceof VArc)) {
                    aResponse.add(obj);
                    continue;
                }

                aResponse.add(obj);
                annoToArcIdx.put(fs, (VArc) obj);

                renderRequiredFeatureErrors(aFeatures, fs, aResponse);
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
                .collect(toList());
        var message = getYieldMessage(sortedYield);
        return Optional.of(message);
    }

    @Override
    public List<VObject> render(VDocument aVDocument, AnnotationFS aFS,
            List<AnnotationFeature> aFeatures, int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aFS.getCAS())) {
            return Collections.emptyList();
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

            return Collections.emptyList();
        }

        var labelFeatures = renderLabelFeatureValues(typeAdapter, aFS, aFeatures);

        if (traits.isRenderArcs()) {
            var objects = new ArrayList<VObject>();
            var arc = VArc.builder().forAnnotation(aFS) //
                    .withLayer(typeAdapter.getLayer()) //
                    .withSource(sourceFs) //
                    .withTarget(targetFs) //
                    .withFeatures(labelFeatures) //
                    .build();
            objects.add(arc);

            createDummyEndpoint(sourceFs, aWindowBegin, aWindowEnd, typeAdapter, objects);
            createDummyEndpoint(targetFs, aWindowBegin, aWindowEnd, typeAdapter, objects);

            return objects;
        }

        var governor = (AnnotationFS) sourceFs;
        var dependent = (AnnotationFS) targetFs;

        var noteBuilder = new StringBuilder();
        noteBuilder.append(typeAdapter.getLayer().getUiName());
        noteBuilder.append("\n");
        noteBuilder.append(governor.getCoveredText());
        noteBuilder.append(" -> ");
        noteBuilder.append(dependent.getCoveredText());
        noteBuilder.append("\n");

        for (Entry<String, String> entry : labelFeatures.entrySet()) {
            noteBuilder.append(entry.getKey());
            noteBuilder.append(" = ");
            noteBuilder.append(entry.getValue());
            noteBuilder.append("\n");
        }

        String note = noteBuilder.toString().stripTrailing();
        aVDocument.add(new VComment(sourceFs, VCommentType.INFO, "\n⬆️ " + note));
        aVDocument.add(new VComment(targetFs, VCommentType.INFO, "\n⬇️ " + note));

        return Collections.emptyList();
    }

    private void createDummyEndpoint(FeatureStructure aEndpoint, int aWindowBegin, int aWindowEnd,
            RelationAdapter aTypeAdapter, List<VObject> aObjects)
    {
        if (((AnnotationFS) aEndpoint).getEnd() < aWindowBegin) {
            aObjects.add(VSpan.builder().forAnnotation((AnnotationFS) aEndpoint) //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(0, 0)) //
                    .build());
        }
        if (((AnnotationFS) aEndpoint).getBegin() >= aWindowEnd) {
            aObjects.add(VSpan.builder().forAnnotation((AnnotationFS) aEndpoint) //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(aWindowEnd, aWindowEnd)) //
                    .build());
        }
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(CAS aCas, VID aVid)
    {
        if (!checkTypeSystem(aCas)) {
            return Collections.emptyList();
        }

        var fs = ICasUtil.selectAnnotationByAddr(aCas, aVid.getId());

        var group = new VLazyDetailGroup();

        var dependentFs = getTargetFs(fs);
        if (dependentFs instanceof AnnotationFS) {
            group.addDetail(new VLazyDetail("Target",
                    abbreviate(((AnnotationFS) dependentFs).getCoveredText(), 300)));
        }

        var governorFs = getSourceFs(fs);
        if (governorFs instanceof AnnotationFS) {
            group.addDetail(new VLazyDetail("Origin",
                    abbreviate(((AnnotationFS) governorFs).getCoveredText(), 300)));
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
